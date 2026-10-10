package pe.imperio.gym.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.imperio.gym.data.Store;
import pe.imperio.gym.domain.Attendance;
import pe.imperio.gym.domain.AuditEvent;
import pe.imperio.gym.domain.Branch;
import pe.imperio.gym.domain.Membership;
import pe.imperio.gym.domain.UserAccount;

@Service @Transactional(readOnly=true)
public class BranchService {
 private final Store db;
 private final Clock clock;
 public BranchService(Store db,Clock clock){this.db=db;this.clock=clock;}

 public record Summary(Branch branch,long activeMembers,long pendingPayments,long expiredMembers,long present) {
  public long available(){return Math.max(0,branch.capacity-present);}
 }
 public record Presence(Attendance attendance,UserAccount member,Branch branch) {}

 public List<Summary> summaries(){
  LocalDate today=LocalDate.now(clock);
  return db.list(Branch.class,"select b from Branch b order by b.id").stream().map(branch->new Summary(branch,
   db.count("select count(m) from Membership m where m.branchId=?1 and m.startedOn<=?2 and m.expiresOn>?2",branch.id,today),
   db.count("select count(p) from Payment p where p.branchId=?1 and p.status='PENDING'",branch.id),
   db.count("select count(m) from Membership m where m.branchId=?1 and m.expiresOn<=?2",branch.id,today),
   db.count("select count(a) from Attendance a where a.branchId=?1 and a.checkedOutAt is null",branch.id)
  )).toList();
 }

 public List<Presence> presentAt(Long branchId){
  var query=branchId==null?"select a from Attendance a where a.checkedOutAt is null order by a.checkedInAt desc":
   "select a from Attendance a where a.branchId=?1 and a.checkedOutAt is null order by a.checkedInAt desc";
  var records=branchId==null?db.list(Attendance.class,query):db.list(Attendance.class,query,branchId);
  return records.stream().map(a->new Presence(a,db.get(UserAccount.class,a.userId),db.get(Branch.class,a.branchId))).toList();
 }

 public long unassignedActiveMembers(){
  return db.count("select count(m) from Membership m where m.branchId is null and m.startedOn<=?1 and m.expiresOn>?1",LocalDate.now(clock));
 }

 @Transactional
 @PreAuthorize("hasRole('ADMIN')")
 public void checkIn(String dni,Long branchId,Long administratorId){
  if(dni==null||!dni.strip().matches("\\d{8}"))throw new IllegalArgumentException("Ingresa un DNI válido de 8 dígitos.");
  if(branchId==null)throw new IllegalArgumentException("Selecciona una sede.");
  var branch=db.lock(Branch.class,branchId);
  if(!branch.active)throw new IllegalArgumentException("La sede seleccionada no está disponible.");
  var member=db.one(UserAccount.class,"select u from UserAccount u where u.dni=?1",dni.strip())
   .orElseThrow(()->new IllegalArgumentException("No se encontró un socio con ese DNI."));
  db.lock(UserAccount.class,member.id);
  if(db.one(Attendance.class,"select a from Attendance a where a.userId=?1 and a.checkedOutAt is null",member.id).isPresent())
   throw new IllegalArgumentException("El socio ya tiene una entrada sin registrar su salida.");
  var membership=db.one(Membership.class,"select m from Membership m where m.userId=?1",member.id)
   .orElseThrow(()->new IllegalArgumentException("El socio no tiene una membresía activa."));
  LocalDate today=LocalDate.now(clock);
  if(membership.startedOn.isAfter(today)||!membership.expiresOn.isAfter(today))
   throw new IllegalArgumentException("La membresía del socio no está vigente.");
  if(!branch.id.equals(membership.branchId))
   throw new IllegalArgumentException("La membresía del socio no corresponde a esta sede.");
  long present=db.count("select count(a) from Attendance a where a.branchId=?1 and a.checkedOutAt is null",branchId);
  if(present>=branch.capacity)throw new IllegalArgumentException("La sede está llena; no se puede registrar otra entrada.");
  var attendance=new Attendance();attendance.branchId=branchId;attendance.userId=member.id;attendance.checkedInAt=clock.instant();db.save(attendance);
  audit(administratorId,"MEMBER_CHECK_IN",attendance.id,member,branch);
 }

 @Transactional
 @PreAuthorize("hasRole('ADMIN')")
 public void checkOut(Long attendanceId,Long administratorId){
  var initial=db.get(Attendance.class,attendanceId);
  db.lock(Branch.class,initial.branchId);
  db.lock(UserAccount.class,initial.userId);
  var attendance=db.lock(Attendance.class,attendanceId);
  if(attendance.checkedOutAt!=null)throw new IllegalArgumentException("La salida ya fue registrada.");
  attendance.checkedOutAt=clock.instant();
  audit(administratorId,"MEMBER_CHECK_OUT",attendance.id,db.get(UserAccount.class,attendance.userId),db.get(Branch.class,attendance.branchId));
 }

 private void audit(Long administratorId,String action,Long attendanceId,UserAccount member,Branch branch){
  var event=new AuditEvent();event.actorId=administratorId;event.action=action;event.targetType="attendance";event.targetId=attendanceId;
  event.detail=member.name+" ("+member.dni+") · "+branch.name;event.createdAt=Instant.now(clock);db.save(event);
 }
}
