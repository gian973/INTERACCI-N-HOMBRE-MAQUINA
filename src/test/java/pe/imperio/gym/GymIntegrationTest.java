package pe.imperio.gym;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import pe.imperio.gym.data.*;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.service.*;
import pe.imperio.gym.web.Forms;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class GymIntegrationTest {
 @Autowired MockMvc mvc;@Autowired GymService gym;@Autowired IdentityService identity;@Autowired Store db;@Autowired UserRepository users;
 @Autowired ReminderService reminders;
 static final AtomicInteger sequence=new AtomicInteger(20000000+new java.security.SecureRandom().nextInt(9000000));
 UserAccount customer(){String dni=Integer.toString(sequence.incrementAndGet());return identity.register(new Forms.Registration("Cliente Test",dni,dni+"@example.test","+51999000000","PasswordTest2026!","PasswordTest2026!"));}
 Long admin(){return users.findByEmailIgnoreCaseOrDni("admin@imperio.test","10000000").orElseThrow().id;}
 MockMultipartFile proof(){return new MockMultipartFile("proof","receipt.pdf","application/pdf","%PDF-1.4\n1 0 obj <<>> endobj\n%%EOF".getBytes());}
 @Test void calendarMonthsClampWithoutOverflow(){assertThat(GymService.addMonths(LocalDate.of(2026,1,31),1)).isEqualTo("2026-02-28");assertThat(GymService.addMonths(LocalDate.of(2024,1,31),1)).isEqualTo("2024-02-29");assertThat(GymService.addMonths(LocalDate.of(2026,1,31),2)).isEqualTo("2026-03-31");}
 @Test void visitorRegistrationPreservesPlanAndCardPaymentIsIdempotent() throws Exception {
  var session=new MockHttpSession();
  mvc.perform(get("/choose/2").session(session)).andExpect(redirectedUrl("/checkout"));
  mvc.perform(get("/checkout").session(session)).andExpect(status().is3xxRedirection());
  String dni=Integer.toString(sequence.incrementAndGet());
  mvc.perform(post("/register").session(session).with(csrf()).param("name","Nuevo Socio").param("dni",dni).param("email",dni+"@example.test").param("phone","+51999000000").param("password","PasswordTest2026!").param("confirmation","PasswordTest2026!")).andExpect(redirectedUrl("/login"));
  mvc.perform(post("/login").session(session).with(csrf()).param("login",dni).param("password","PasswordTest2026!")).andExpect(redirectedUrl("/checkout"));
  String email=dni+"@example.test";mvc.perform(get("/checkout").session(session).with(user(email).roles("CLIENTE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("114.99")));
  var draft=(GymService.Draft)session.getAttribute("draft");
  var first=mvc.perform(multipart("/checkout").session(session).with(user(email).roles("CLIENTE")).with(csrf()).param("key",draft.key()).param("method","CARD_TEST").param("accepted","true")).andExpect(status().is3xxRedirection()).andReturn().getResponse().getRedirectedUrl();
  mvc.perform(multipart("/checkout").session(session).with(user(email).roles("CLIENTE")).with(csrf()).param("key",draft.key()).param("method","CARD_TEST").param("accepted","true")).andExpect(redirectedUrl(first));
  var u=users.findByEmailIgnoreCaseOrDni(email,dni).orElseThrow();assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(gym.today().plusMonths(2));
  assertThat(db.count("select count(p) from Payment p where p.userId=?1",u.id)).isEqualTo(1);
 }
 @Test @WithMockUser(username="admin@imperio.test",roles="ADMIN")
 void manualConfirmationRenewalAndRejectionAreAtomic() throws Exception {
  var u=customer();var first=gym.pay(u.id,gym.draft(1L),"WALLET","OP-"+UUID.randomUUID(),proof());assertThat(gym.membership(u.id)).isEmpty();
  gym.reviewPayment(first.id,admin(),true,"Validado");LocalDate expiry=gym.membership(u.id).orElseThrow().expiresOn;
  var renewal=gym.pay(u.id,gym.draft(2L),"TRANSFER","OP-"+UUID.randomUUID(),proof());assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry);
  gym.reviewPayment(renewal.id,admin(),true,"Renovación validada");assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
  assertThatThrownBy(()->gym.reviewPayment(renewal.id,admin(),true,"Otra vez")).isInstanceOf(IllegalArgumentException.class);
  assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
  var rejected=gym.pay(u.id,gym.draft(3L),"WALLET","OP-"+UUID.randomUUID(),proof());assertThatThrownBy(()->gym.reviewPayment(rejected.id,admin(),false,"")).isInstanceOf(IllegalArgumentException.class);
  gym.reviewPayment(rejected.id,admin(),false,"Comprobante no válido");assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry.plusMonths(2));
  mvc.perform(get("/account/payments/"+renewal.id).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmado")));
 }
 @Test @WithMockUser(username="admin@imperio.test",roles="ADMIN")
 void freezeRequiresReviewAndResponseIsVisible() throws Exception {
  var u=customer();gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);var expiry=gym.membership(u.id).orElseThrow().expiresOn;
  var r=gym.request(u.id,new Forms.Freeze("TRAVEL",gym.today().plusDays(1),gym.today().plusDays(3),"Viaje de prueba"),null);
  assertThatThrownBy(()->gym.reviewRequest(r.id,admin(),"APPROVED","")).isInstanceOf(IllegalArgumentException.class);
  gym.reviewRequest(r.id,admin(),"IN_REVIEW","Documentos en revisión");gym.reviewRequest(r.id,admin(),"APPROVED","Solicitud aprobada");
  assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(expiry);
  assertThatThrownBy(()->gym.reviewRequest(r.id,admin(),"APPROVED","")).isInstanceOf(IllegalArgumentException.class);
  mvc.perform(get("/account/requests/"+r.id).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Solicitud aprobada")));
  assertThatThrownBy(()->gym.request(u.id,new Forms.Freeze("TRAVEL",gym.today().plusDays(2),gym.today().plusDays(4),"Superpuesto"),null)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void preferencesRescheduleAndCancelOldExpiry(){
  var u=customer();gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);var old=gym.membership(u.id).orElseThrow().expiresOn;
  assertThat(db.count("select count(r) from Reminder r where r.userId=?1",u.id)).isZero();
  gym.preferences(u.id,new Forms.Preferences("+51999000000",true,true,true));assertThat(db.count("select count(r) from Reminder r where r.userId=?1 and r.status='SCHEDULED'",u.id)).isEqualTo(2);
  gym.pay(u.id,gym.draft(2L),"CARD_TEST",null,null);assertThat(db.count("select count(r) from Reminder r where r.userId=?1 and r.expiryDate=?2 and r.status='CANCELLED'",u.id,old)).isEqualTo(2);
  gym.preferences(u.id,new Forms.Preferences("+51999000000",false,true,true));assertThat(db.count("select count(r) from Reminder r where r.userId=?1 and r.status='SCHEDULED'",u.id)).isZero();
 }
 @Test void permissionsCsrfAndFilesAreEnforced() throws Exception {
  var u=customer();var another=customer();var p=gym.pay(u.id,gym.draft(1L),"WALLET","OP-"+UUID.randomUUID(),proof());var f=db.one(PrivateFile.class,"select f from PrivateFile f where f.paymentId=?1",p.id).orElseThrow();
  mvc.perform(get("/admin").with(user(u.email).roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get("/account/payments/"+p.id).with(user(another.email).roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get("/files/"+f.id).with(user(another.email).roles("CLIENTE"))).andExpect(status().isForbidden());
  mvc.perform(get("/files/"+f.id).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
  mvc.perform(post("/account/preferences").with(user(u.email).roles("CLIENTE")).param("phone","+51999000000")).andExpect(status().isForbidden());
  mvc.perform(get("/account").with(user("admin@imperio.test").roles("ADMIN"))).andExpect(status().isForbidden());
 }
 @Test void uploadsAndDatesAreValidatedAndRolledBack(){
  var u=customer();var invalid=new MockMultipartFile("proof","fake.pdf","application/pdf","<script>alert(1)</script>".getBytes());
  assertThatThrownBy(()->gym.pay(u.id,gym.draft(1L),"WALLET","OP-"+UUID.randomUUID(),invalid)).isInstanceOf(IllegalArgumentException.class);
  assertThat(db.count("select count(p) from Payment p where p.userId=?1",u.id)).isZero();
  gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);
  assertThatThrownBy(()->gym.request(u.id,new Forms.Freeze("TRAVEL",gym.today().minusDays(1),gym.today(),"Pasado"),null)).isInstanceOf(IllegalArgumentException.class);
  assertThatThrownBy(()->gym.request(u.id,new Forms.Freeze("HEALTH",gym.today(),gym.today().plusDays(1),"Sin documento"),null)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void recoveryTokenIsSingleUseAndHashed(){
  var u=customer();String link=identity.forgot(u.email);String raw=link.substring(link.indexOf("token=")+6);
  var token=db.one(ResetToken.class,"select t from ResetToken t where t.userId=?1",u.id).orElseThrow();assertThat(token.tokenHash).isNotEqualTo(raw);assertThat(token.expiresAt).isAfter(Instant.now());
  identity.reset(raw,"ChangedPassword2026!","ChangedPassword2026!");assertThatThrownBy(()->identity.reset(raw,"ChangedAgain2026!","ChangedAgain2026!")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test @WithMockUser(username="admin@imperio.test",roles="ADMIN")
 void catalogChangesKeepContractHistory(){
  var u=customer();var plan=new Forms.PlanEdit("Plan test",2,new BigDecimal("114.99"),true);
  var p=gym.savePlan(null,plan,List.of(1L,2L),admin());var payment=gym.pay(u.id,gym.draft(p.id),"CARD_TEST",null,null);
  gym.savePlan(p.id,new Forms.PlanEdit("Editado",3,new BigDecimal("200.00"),false),List.of(3L),admin());
  var historical=db.get(Payment.class,payment.id);assertThat(historical.amount).isEqualByComparingTo("114.99");assertThat(historical.months).isEqualTo(2);assertThat(historical.planName).isEqualTo("Plan test");
  assertThat(db.count("select count(b) from ContractBenefit b where b.paymentId=?1",payment.id)).isEqualTo(2);assertThatThrownBy(()->gym.draft(p.id)).isInstanceOf(IllegalArgumentException.class);
 }
 @Test void concurrentIdenticalPaymentAppliesOnce() throws Exception {
  var u=customer();var draft=gym.draft(1L);
  try(var pool=Executors.newFixedThreadPool(2)){var a=pool.submit(()->gym.pay(u.id,draft,"CARD_TEST",null,null));var b=pool.submit(()->gym.pay(u.id,draft,"CARD_TEST",null,null));assertThat(a.get(15,TimeUnit.SECONDS).id).isEqualTo(b.get(15,TimeUnit.SECONDS).id);}
  assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(gym.today().plusMonths(1));
 }
 @Test void dueRemindersAreSimulatedOnceAndExpiredTokensFail(){
  var u=customer();gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);
  db.execute("update Membership m set m.expiresOn=?1 where m.userId=?2",gym.today().plusDays(7),u.id);
  gym.preferences(u.id,new Forms.Preferences("+51999000000",true,true,true));
  var r=db.one(Reminder.class,"select r from Reminder r where r.userId=?1 and r.daysBefore=7",u.id).orElseThrow();
  reminders.deliver(r.id);assertThat(db.get(Reminder.class,r.id).status).isEqualTo("SIMULATED");var processed=db.get(Reminder.class,r.id).processedAt;
  reminders.deliver(r.id);assertThat(db.get(Reminder.class,r.id).processedAt).isEqualTo(processed);
  String link=identity.forgot(u.email);String raw=link.substring(link.indexOf("token=")+6);
  db.execute("update ResetToken t set t.expiresAt=?1 where t.userId=?2",Instant.now().minusSeconds(60),u.id);
  assertThatThrownBy(()->identity.reset(raw,"ExpiredToken2026!","ExpiredToken2026!")).isInstanceOf(IllegalArgumentException.class);
 }
 @Test @WithMockUser(username="admin@imperio.test",roles="ADMIN")
 void configurableFreezeExtendsExactlyOnce() throws Exception {
  Object target=org.springframework.test.util.AopTestUtils.getTargetObject(gym);
  org.springframework.test.util.ReflectionTestUtils.setField(target,"freezePolicy","EXTEND_DAYS");
  try{
   var u=customer();gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);var original=gym.membership(u.id).orElseThrow().expiresOn;
   var r=gym.request(u.id,new Forms.Freeze("TRAVEL",gym.today().plusDays(1),gym.today().plusDays(3),"Prueba de política"),null);
   gym.reviewRequest(r.id,admin(),"IN_REVIEW","Revisando");gym.reviewRequest(r.id,admin(),"APPROVED","Aprobado");
   assertThat(gym.membership(u.id).orElseThrow().expiresOn).isEqualTo(original.plusDays(3));
   assertThatThrownBy(()->gym.reviewRequest(r.id,admin(),"APPROVED","")).isInstanceOf(IllegalArgumentException.class);
  }finally{org.springframework.test.util.ReflectionTestUtils.setField(target,"freezePolicy","NO_EXTENSION");}
 }
 @Test @WithMockUser(username="admin@imperio.test",roles="ADMIN")
 void concurrentDifferentManualApprovalsDoNotLoseMonths() throws Exception {
  var u=customer();gym.pay(u.id,gym.draft(1L),"CARD_TEST",null,null);var original=gym.membership(u.id).orElseThrow().expiresOn;
  var a=gym.pay(u.id,gym.draft(1L),"WALLET","CON-"+UUID.randomUUID(),proof());
  var b=gym.pay(u.id,gym.draft(2L),"WALLET","CON-"+UUID.randomUUID(),proof());
  var context=org.springframework.security.core.context.SecurityContextHolder.getContext();var actor=admin();
  try(var pool=Executors.newFixedThreadPool(2)){
   var first=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<Boolean>(()->{gym.reviewPayment(a.id,actor,true,"A");return true;},context));
   var second=pool.submit(new org.springframework.security.concurrent.DelegatingSecurityContextCallable<Boolean>(()->{gym.reviewPayment(b.id,actor,true,"B");return true;},context));
   assertThat(first.get(20,TimeUnit.SECONDS)).isTrue();assertThat(second.get(20,TimeUnit.SECONDS)).isTrue();
  }
  assertThat(gym.membership(u.id).orElseThrow().expiresOn).isIn(original.plusMonths(1).plusMonths(2),original.plusMonths(2).plusMonths(1));
 }
 @Test void pagesRenderForBothRoles() throws Exception {
  mvc.perform(post("/register").with(csrf()).param("name","Datos incompletos").param("email","incompleto@example.test").param("confirmation","NoPassword2026!")).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","dni","phone","password"));
  var u=customer();
  for(String path:List.of("/","/login","/register","/forgot","/reset?token=invalid"))mvc.perform(get(path)).andExpect(status().isOk());
  for(String path:List.of("/account","/account/renew","/account/payments","/account/requests","/account/preferences"))mvc.perform(get(path).with(user(u.email).roles("CLIENTE"))).andExpect(status().isOk());
  for(String path:List.of("/admin","/admin/clients","/admin/payments","/admin/requests","/admin/plans","/admin/reminders","/admin/audit","/admin/plan","/admin/plans/1","/admin/clients/"+u.id))mvc.perform(get(path).with(user("admin@imperio.test").roles("ADMIN"))).andExpect(status().isOk());
 }
}
