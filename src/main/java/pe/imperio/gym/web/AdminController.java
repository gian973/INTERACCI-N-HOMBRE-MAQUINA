package pe.imperio.gym.web;
import pe.imperio.gym.data.*;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import java.time.*;
import java.util.*;
@Controller @RequestMapping("/admin")
public class AdminController {
 private final Store db;private final GymService gym;private final IdentityService identity;private final ClientController client;
 public AdminController(Store db,GymService gym,IdentityService identity,ClientController client){this.db=db;this.gym=gym;this.identity=identity;this.client=client;}
 @GetMapping String dashboard(Model m){
  m.addAttribute("customers",db.count("select count(u) from UserAccount u where exists(select r from UserRoleEntry r where r.userId=u.id and r.roleId=1)"));
  m.addAttribute("active",db.count("select count(m) from Membership m where m.expiresOn>?1",gym.today()));
  m.addAttribute("expired",db.count("select count(m) from Membership m where m.expiresOn<=?1",gym.today()));
  m.addAttribute("pending",db.count("select count(p) from Payment p where p.status='PENDING'"));
  m.addAttribute("requestsCount",db.count("select count(r) from FreezeRequest r where r.status in ('SENT','IN_REVIEW')"));return "admin/dashboard";
 }
 @GetMapping("/{section:clients|payments|requests|plans|reminders|audit}") String list(@PathVariable String section,@RequestParam(defaultValue="") String q,
  @RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String kind,@RequestParam(required=false) LocalDate from,@RequestParam(required=false) LocalDate to,
  @RequestParam(defaultValue="0") int page,Model m){
  if(q.length()>100||status.length()>30||kind.length()>20)throw new IllegalArgumentException("Filtro inválido.");
  page=Math.max(0,Math.min(page,100000));String entity=switch(section){case "clients"->"UserAccount";case "payments"->"Payment";case "requests"->"FreezeRequest";case "plans"->"Plan";case "reminders"->"Reminder";default->"AuditEvent";};
  String where=" where 1=1";Map<String,Object> params=new HashMap<>();
  if(section.equals("clients"))where+=" and exists(select ur from UserRoleEntry ur where ur.userId=e.id and ur.roleId=1)";
  if(!q.isBlank()){
   params.put("q","%"+q.toLowerCase(Locale.ROOT)+"%");
   where+=section.equals("clients")?" and (lower(e.name) like :q or lower(e.email) like :q or e.dni like :q)":section.equals("plans")?" and lower(e.name) like :q":section.equals("audit")?" and lower(e.action) like :q":" and exists(select u from UserAccount u where u.id=e.userId and (lower(u.name) like :q or u.dni like :q or lower(u.email) like :q))";
  }
  if(!status.isBlank()&&Set.of("payments","requests","reminders").contains(section)){where+=" and e.status=:status";params.put("status",status);}
  if(!kind.isBlank()&&section.equals("requests")){where+=" and e.kind=:kind";params.put("kind",kind);}
  if(Set.of("clients","payments","requests","audit","reminders").contains(section)){
   boolean date=Set.of("requests","reminders").contains(section);String field=section.equals("requests")?"startsOn":section.equals("reminders")?"expiryDate":"createdAt";
   if(from!=null){where+=" and e."+field+">=:from";params.put("from",date?from:from.atStartOfDay(ZoneId.of("America/Lima")).toInstant());}
   if(to!=null){where+=" and e."+field+(date?"<=:to":"<:to");params.put("to",date?to:to.plusDays(1).atStartOfDay(ZoneId.of("America/Lima")).toInstant());}
  }
  var query=db.em().createQuery("select e from "+entity+" e"+where+" order by e.id desc");var count=db.em().createQuery("select count(e) from "+entity+" e"+where,Long.class);
  params.forEach((k,v)->{query.setParameter(k,v);count.setParameter(k,v);});long total=count.getSingleResult();
  m.addAttribute("rows",query.setFirstResult(page*10).setMaxResults(10).getResultList());m.addAttribute("section",section);m.addAttribute("page",page);m.addAttribute("hasNext",total>(page+1)*10);m.addAttribute("total",total);
  m.addAttribute("q",q);m.addAttribute("filterStatus",status);m.addAttribute("kind",kind);m.addAttribute("from",from);m.addAttribute("to",to);return "admin/list";
 }
 @GetMapping("/clients/{id}") String customer(@PathVariable Long id,Model m){m.addAttribute("customer",db.get(UserAccount.class,id));m.addAttribute("membership",gym.membership(id).orElse(null));m.addAttribute("payments",db.list(Payment.class,"select p from Payment p where p.userId=?1 order by p.id desc",id));m.addAttribute("requests",db.list(FreezeRequest.class,"select r from FreezeRequest r where r.userId=?1 order by r.id desc",id));return "admin/client";}
 @GetMapping("/payments/{id}") String payment(@PathVariable Long id,Model m){client.paymentModel(db.get(Payment.class,id),m);return "payment";}
 @PostMapping("/payments/{id}/review") String reviewPayment(@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,Authentication a,RedirectAttributes flash){
  try{gym.reviewPayment(id,identity.current(a).id,approve,comment);flash.addFlashAttribute("success","Revisión registrada.");}catch(IllegalArgumentException e){flash.addFlashAttribute("message",e.getMessage());}return "redirect:/admin/payments/"+id;
 }
 @GetMapping("/requests/{id}") String request(@PathVariable Long id,Model m){client.requestDetailModel(db.get(FreezeRequest.class,id),m);return "request-detail";}
 @PostMapping("/requests/{id}/review") String reviewRequest(@PathVariable Long id,@RequestParam String next,@RequestParam(defaultValue="") String comment,Authentication a,RedirectAttributes flash){
  try{gym.reviewRequest(id,identity.current(a).id,next,comment);flash.addFlashAttribute("success","Resolución registrada y visible para el cliente.");}catch(IllegalArgumentException e){flash.addFlashAttribute("message",e.getMessage());}return "redirect:/admin/requests/"+id;
 }
 @GetMapping("/plan") String newPlan(Model m){m.addAttribute("form",new Forms.PlanEdit("",1,new java.math.BigDecimal("89.99"),true));planExtras(null,m);return "admin/plan";}
 @GetMapping("/plans/{id}") String editPlan(@PathVariable Long id,Model m){var p=db.get(Plan.class,id);m.addAttribute("form",new Forms.PlanEdit(p.name,p.months,p.price,p.active));planExtras(id,m);return "admin/plan";}
 private void planExtras(Long id,Model m){m.addAttribute("planId",id);m.addAttribute("benefits",db.list(Benefit.class,"select b from Benefit b order by b.id"));m.addAttribute("chosen",id==null?List.of():gym.benefits(id));}
 @PostMapping("/plan") String savePlan(@RequestParam(required=false) Long id,@Valid @ModelAttribute("form") Forms.PlanEdit f,BindingResult errors,@RequestParam(required=false) List<Long> benefitIds,Authentication a,Model m,RedirectAttributes flash){
  if(!errors.hasErrors())try{gym.savePlan(id,f,benefitIds,identity.current(a).id);flash.addFlashAttribute("success","Plan guardado. Los contratos históricos conservan sus datos.");return "redirect:/admin/plans";}catch(IllegalArgumentException e){errors.reject("plan",e.getMessage());}
  planExtras(id,m);return "admin/plan";
 }
}
