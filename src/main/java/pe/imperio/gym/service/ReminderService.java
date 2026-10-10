package pe.imperio.gym.service;
import pe.imperio.gym.data.Store;
import pe.imperio.gym.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
@Service @Transactional
public class ReminderService {
 private final Store db;private final Clock clock;private final WhatsAppProvider provider;
 public ReminderService(Store db,Clock clock,WhatsAppProvider provider){this.db=db;this.clock=clock;this.provider=provider;}
 public void refresh(Long user,LocalDate expiry){
  db.execute("update Reminder r set r.status='CANCELLED' where r.userId=?1 and r.status='SCHEDULED'",user);
  var p=db.currentOne(Preference.class,"select p from Preference p where p.userId=?1",user).orElseThrow();
  if(!p.consent||expiry==null)return;
  for(int days:new int[]{7,1}){
   if(days==7&&!p.notice7||days==1&&!p.notice1||expiry.minusDays(days).isBefore(LocalDate.now(clock)))continue;
   var old=db.one(Reminder.class,"select r from Reminder r where r.userId=?1 and r.expiryDate=?2 and r.daysBefore=?3",user,expiry,days);
   if(old.isPresent()){db.em().refresh(old.get());if(old.get().status.equals("CANCELLED"))old.get().status="SCHEDULED";continue;}
   var r=new Reminder();r.userId=user;r.expiryDate=expiry;r.daysBefore=days;r.status="SCHEDULED";db.save(r);
  }
 }
 public void deliver(Long id){
  var original=db.get(Reminder.class,id);var u=db.lock(UserAccount.class,original.userId);var r=db.lock(Reminder.class,id);db.em().refresh(r);
  if(!r.status.equals("SCHEDULED")||r.getDueOn().isAfter(LocalDate.now(clock)))return;
  var p=db.currentOne(Preference.class,"select p from Preference p where p.userId=?1",r.userId).orElseThrow();
  var m=db.currentOne(Membership.class,"select m from Membership m where m.userId=?1",r.userId);
  if(!p.consent||m.isEmpty()||!m.get().expiresOn.equals(r.expiryDate)||r.daysBefore==7&&!p.notice7||r.daysBefore==1&&!p.notice1||!r.getDueOn().equals(LocalDate.now(clock)))r.status="CANCELLED";
  else r.status=provider.send(p.phone,u.name,r.expiryDate,r.daysBefore,"reminder-"+r.id);
  r.processedAt=clock.instant();
 }
}
