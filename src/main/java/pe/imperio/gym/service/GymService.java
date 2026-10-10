package pe.imperio.gym.service;
import pe.imperio.gym.data.*;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.web.Forms;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.util.*;
@Service @Transactional
public class GymService {
 private final Store db; private final FileService files; private final ReminderService reminders; private final Clock clock;
 @Value("${app.freeze-policy:NO_EXTENSION}") String freezePolicy;
 public GymService(Store db,FileService files,ReminderService reminders,Clock clock){this.db=db;this.files=files;this.reminders=reminders;this.clock=clock;}
 public LocalDate today(){return LocalDate.now(clock);}
 public Optional<Membership> membership(Long user){return db.one(Membership.class,"select m from Membership m where m.userId=?1",user);}
 public List<String> benefits(Long plan){return db.em().createNativeQuery("select b.label from benefits b join plan_benefits pb on pb.benefit_id=b.id where pb.plan_id=?1 order by b.id",String.class).setParameter(1,plan).getResultList();}
 public record Draft(Long planId,String name,int months,BigDecimal price,List<String> benefits,String key) implements java.io.Serializable {}
 public Draft draft(Long planId){var p=db.get(Plan.class,planId);if(!p.active)throw new IllegalArgumentException("Este plan ya no está disponible.");return new Draft(p.id,p.name,p.months,p.price,benefits(p.id),UUID.randomUUID().toString());}
 public LocalDate proposedFrom(Long user){return membership(user).map(m->m.expiresOn.isAfter(today())?m.expiresOn:today()).orElse(today());}
 public static LocalDate addMonths(LocalDate base,int months){return base.plusMonths(months);}
 public Payment pay(Long user,Draft draft,String method,String operation,MultipartFile proof){
  db.lock(UserAccount.class,user);
  var existing=db.one(Payment.class,"select p from Payment p where p.idempotencyKey=?1",draft.key());
  if(existing.isPresent()){owned(existing.get().userId,user);return existing.get();}
  if(!Set.of("CARD_TEST","WALLET","TRANSFER").contains(method))throw new IllegalArgumentException("Selecciona un método válido.");
  var plan=db.get(Plan.class,draft.planId());if(!plan.active)throw new IllegalArgumentException("El plan fue desactivado. Elige otro.");
  if(!method.equals("CARD_TEST")&&(operation==null||!operation.matches("[A-Za-z0-9-]{4,80}")))throw new IllegalArgumentException("Ingresa un número de operación de 4 a 80 caracteres.");
  if(!method.equals("CARD_TEST")&&db.count("select count(p) from Payment p where p.method=?1 and p.operationNumber=?2",method,operation)>0)throw new IllegalArgumentException("Ese número de operación ya fue registrado.");
  var p=new Payment();p.userId=user;p.planId=draft.planId();p.planName=draft.name();p.months=draft.months();p.amount=draft.price();p.method=method;p.status="PENDING";p.operationNumber=method.equals("CARD_TEST")?null:operation;p.idempotencyKey=draft.key();p.createdAt=clock.instant();db.save(p);
  for(String label:draft.benefits()){var b=new ContractBenefit();b.paymentId=p.id;b.label=label;db.save(b);}
  if(!method.equals("CARD_TEST"))files.save(proof,user,p.id,null);
  audit(user,"PAYMENT_CREATED","payment",p.id,method);
  if(method.equals("CARD_TEST")){apply(p);audit(user,"SIMULATED_PAYMENT_CONFIRMED","payment",p.id,"Tarjeta de prueba; no se realizó un cobro.");}
  return p;
 }
 private void apply(Payment p){
  if(!p.status.equals("PENDING"))throw new IllegalArgumentException("El pago ya fue resuelto.");
  var m=db.currentOne(Membership.class,"select m from Membership m where m.userId=?1",p.userId).orElseGet(()->{var n=new Membership();n.userId=p.userId;n.startedOn=today();return n;});
  LocalDate base=m.expiresOn!=null&&m.expiresOn.isAfter(today())?m.expiresOn:today();
  if(m.expiresOn!=null&&!m.expiresOn.isAfter(today()))m.startedOn=today();
  m.paymentId=p.id;m.expiresOn=addMonths(base,p.months);p.appliedFrom=base;p.status="CONFIRMED";if(m.id==null)db.save(m);reminders.refresh(p.userId,m.expiresOn);
 }
 @PreAuthorize("hasRole('ADMIN')")
 public void reviewPayment(Long id,Long admin,boolean approve,String comment){
  var initial=db.get(Payment.class,id);db.lock(UserAccount.class,initial.userId);var p=db.lock(Payment.class,id);db.em().refresh(p);
  if(!p.status.equals("PENDING"))throw new IllegalArgumentException("El pago ya fue resuelto; no se aplicó otra vez.");
  validateComment(comment,!approve);
  review(p.id,null,admin,p.status,approve?"CONFIRMED":"REJECTED",comment);
  if(approve)apply(p);else p.status="REJECTED";
  audit(admin,"PAYMENT_REVIEW","payment",p.id,p.status);
 }
 public FreezeRequest request(Long user,Forms.Freeze f,MultipartFile attachment){
  db.lock(UserAccount.class,user);
  var m=membership(user).orElseThrow(()->new IllegalArgumentException("Necesitas una membresía para solicitar congelamiento."));
  if(f.startsOn()==null||f.endsOn()==null||f.startsOn().isBefore(today())||f.endsOn().isBefore(f.startsOn())||!f.endsOn().isBefore(m.expiresOn))throw new IllegalArgumentException("Las fechas deben ser futuras, ordenadas y estar dentro de la vigencia.");
  if(db.count("select count(r) from FreezeRequest r where r.userId=?1 and r.status<>'REJECTED' and r.startsOn<=?2 and r.endsOn>=?3",user,f.endsOn(),f.startsOn())>0)throw new IllegalArgumentException("Las fechas coinciden con otra solicitud.");
  var r=new FreezeRequest();r.userId=user;r.kind=f.kind();r.startsOn=f.startsOn();r.endsOn=f.endsOn();r.comment=f.comment();r.status="SENT";r.createdAt=clock.instant();db.save(r);
  if(f.kind().equals("HEALTH")||(attachment!=null&&!attachment.isEmpty()))files.save(attachment,user,null,r.id);
  audit(user,"REQUEST_SENT","request",r.id,r.kind);return r;
 }
 @PreAuthorize("hasRole('ADMIN')")
 public void reviewRequest(Long id,Long admin,String next,String comment){
  var initial=db.get(FreezeRequest.class,id);db.lock(UserAccount.class,initial.userId);var r=db.lock(FreezeRequest.class,id);db.em().refresh(r);
  boolean valid=r.status.equals("SENT")&&next.equals("IN_REVIEW")||r.status.equals("IN_REVIEW")&&Set.of("APPROVED","REJECTED").contains(next);
  if(!valid)throw new IllegalArgumentException("Transición inválida. Primero pasa la solicitud a En revisión; una resolución no puede repetirse.");
  validateComment(comment,next.equals("REJECTED"));
  if(next.equals("APPROVED")){
   if(!Set.of("NO_EXTENSION","EXTEND_DAYS").contains(freezePolicy))throw new IllegalArgumentException("Configura una política de congelamiento válida.");
   r.policy=freezePolicy;
   if(freezePolicy.equals("EXTEND_DAYS")){
    var m=db.currentOne(Membership.class,"select m from Membership m where m.userId=?1",r.userId).orElseThrow();if(!r.endsOn.isBefore(m.expiresOn))throw new IllegalArgumentException("El período solicitado excede la membresía.");
    m.expiresOn=m.expiresOn.plusDays(r.getExtensionDays());reminders.refresh(r.userId,m.expiresOn);
   }
  }
  review(null,r.id,admin,r.status,next,comment);r.status=next;audit(admin,"REQUEST_REVIEW","request",r.id,next+"; policy="+r.policy+"; days="+r.getExtensionDays());
 }
 public void preferences(Long user,Forms.Preferences form){
  db.lock(UserAccount.class,user);var p=db.one(Preference.class,"select p from Preference p where p.userId=?1",user).orElseThrow();
  p.phone=form.phone();p.consent=form.consent();p.notice7=form.notice7();p.notice1=form.notice1();
  reminders.refresh(user,membership(user).map(m->m.expiresOn).orElse(null));
  audit(user,"PREFERENCES_SAVED","user",user,"consent="+p.consent);
 }
 @PreAuthorize("hasRole('ADMIN')")
 public Plan savePlan(Long id,Forms.PlanEdit f,List<Long> benefits,Long admin){
  if(benefits==null||benefits.isEmpty())throw new IllegalArgumentException("Selecciona al menos un beneficio.");
  for(Long b:benefits)db.get(Benefit.class,b);
  Plan p=id==null?new Plan():db.lock(Plan.class,id);
  String previous=id==null?"nuevo":p.name+" / "+p.months+" / "+p.price+" / active="+p.active+" / "+benefits(p.id);
  p.name=f.name();p.months=f.months();p.price=f.price();p.active=f.active();if(id==null)db.save(p);
  db.em().createNativeQuery("delete from plan_benefits where plan_id=?1").setParameter(1,p.id).executeUpdate();
  for(Long b:new HashSet<>(benefits))db.em().createNativeQuery("insert into plan_benefits(plan_id,benefit_id) values(?1,?2)").setParameter(1,p.id).setParameter(2,b).executeUpdate();
  audit(admin,"PLAN_SAVED","plan",p.id,previous+" -> "+p.name+" / "+p.months+" / "+p.price+" / active="+p.active+" / "+benefits);return p;
 }
 public void owned(Long owner,Long user){if(!owner.equals(user))throw new org.springframework.security.access.AccessDeniedException("No puedes acceder a este registro.");}
 private void validateComment(String c,boolean required){if(c==null||c.length()>1000||(required&&c.isBlank()))throw new IllegalArgumentException("Incluye un motivo de rechazo (máximo 1000 caracteres).");}
 private void review(Long payment,Long request,Long actor,String from,String to,String comment){var r=new Review();r.paymentId=payment;r.requestId=request;r.reviewerId=actor;r.fromStatus=from;r.toStatus=to;r.comment=comment;r.createdAt=clock.instant();db.save(r);}
 public void audit(Long actor,String action,String type,Long target,String detail){var a=new AuditEvent();a.actorId=actor;a.action=action;a.targetType=type;a.targetId=target;a.detail=detail.length()>1000?detail.substring(0,1000):detail;a.createdAt=clock.instant();db.save(a);}
}
