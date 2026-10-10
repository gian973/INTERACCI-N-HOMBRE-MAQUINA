package pe.imperio.gym.service;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.data.*;
import pe.imperio.gym.web.Forms;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.*;
@Service @Profile({"dev","test"}) @Transactional
public class DemoService {
 private final Store db;private final UserRepository users;private final IdentityService identity;private final GymService gym;
 @Value("${app.demo-password}") String password;
 public DemoService(Store db,UserRepository users,IdentityService identity,GymService gym){this.db=db;this.users=users;this.identity=identity;this.gym=gym;}
 public void seed(){
  if(users.existsByEmailIgnoreCaseOrDni("juan.perez@ejemplo.com","10000001"))return;
  var juan=make("Juan Pérez","10000001","juan.perez@ejemplo.com");make("Ana Torres","10000002","ana.torres@ejemplo.com");
  var admin=make("Administración Demo","10000000","admin@imperio.test");
  db.execute("delete from UserRoleEntry r where r.userId=?1",admin.id);var role=new UserRoleEntry();role.userId=admin.id;role.roleId=2L;db.save(role);
  gym.pay(juan.id,1L,gym.draft(1L),"CARD_TEST",null,null);
 }
 private UserAccount make(String name,String dni,String email){var u=identity.register(new Forms.Registration(name,dni,email,"+51999000000",password,password));u.demo=true;return u;}
 @PreAuthorize("hasRole('ADMIN')") public void reset(Long admin){
  var ids=db.list(Long.class,"select u.id from UserAccount u where u.demo=true");
  for(Long id:ids){
   db.lock(UserAccount.class,id);
   db.execute("delete from Review v where v.paymentId in (select p.id from Payment p where p.userId=?1) or v.requestId in (select r.id from FreezeRequest r where r.userId=?1)",id);
   db.execute("delete from PrivateFile f where f.ownerId=?1",id);
   db.execute("delete from ContractBenefit b where b.paymentId in (select p.id from Payment p where p.userId=?1)",id);
   db.execute("delete from Membership m where m.userId=?1",id);
   db.execute("delete from Payment p where p.userId=?1",id);
   db.execute("delete from FreezeRequest r where r.userId=?1",id);
   db.execute("delete from Reminder r where r.userId=?1",id);
   db.execute("delete from ResetToken t where t.userId=?1",id);
   db.execute("update Preference p set p.consent=false where p.userId=?1",id);
  }
  var juan=users.findByEmailIgnoreCaseOrDni("juan.perez@ejemplo.com","10000001").orElseThrow();gym.pay(juan.id,1L,gym.draft(1L),"CARD_TEST",null,null);
  gym.audit(admin,"DEMO_RESET","user",admin,"Solo cuentas marcadas demo; catálogo, auditoría y cuentas registradas conservados.");
 }
}
