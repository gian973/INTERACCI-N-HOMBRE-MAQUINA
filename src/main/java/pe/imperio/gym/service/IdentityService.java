package pe.imperio.gym.service;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.data.*;
import pe.imperio.gym.web.Forms;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
@Service @Transactional
public class IdentityService {
 private final Store db; private final UserRepository users; private final PasswordEncoder encoder; private final Clock clock;
 private final ObjectProvider<JavaMailSender> mail;
 @Value("${app.development:false}") boolean development;
 @Value("${spring.mail.host:}") String mailHost;
 @Value("${app.public-url}") String publicUrl;
 @Value("${app.mail-from}") String mailFrom;
 public IdentityService(Store db,UserRepository users,PasswordEncoder encoder,Clock clock,ObjectProvider<JavaMailSender> mail){this.db=db;this.users=users;this.encoder=encoder;this.clock=clock;this.mail=mail;}
 public UserAccount current(Authentication a){if(a==null)throw new org.springframework.security.access.AccessDeniedException("Acceso denegado");return users.findByEmailIgnoreCaseOrDni(a.getName(),a.getName()).orElseThrow();}
 public boolean isRegistered(String email,String dni){return users.existsByEmailIgnoreCaseOrDni(email.strip(),dni);}
 public UserAccount register(Forms.Registration f){
  if(!f.password().equals(f.confirmation()))throw new IllegalArgumentException("La confirmación no coincide.");
  if(f.password().equals(f.dni()))throw new IllegalArgumentException("El DNI no puede ser tu contraseña.");
  if(isRegistered(f.email(),f.dni()))throw new IllegalArgumentException("El correo o DNI ya está registrado.");
  var u=new UserAccount();u.name=f.name().strip();u.dni=f.dni();u.email=f.email().strip().toLowerCase(Locale.ROOT);u.phone=f.phone();u.passwordHash=encoder.encode(f.password());u.createdAt=clock.instant();db.save(u);
  var role=new UserRoleEntry();role.userId=u.id;role.roleId=1L;db.save(role);
  var p=new Preference();p.userId=u.id;p.phone=u.phone;p.consent=false;p.notice7=true;p.notice1=true;db.save(p);return u;
 }
 public String forgot(String login){
  var found=users.findByEmailIgnoreCaseOrDni(login.strip(),login.strip());if(found.isEmpty())return null;
  var u=db.lock(UserAccount.class,found.get().id);
  db.execute("update ResetToken t set t.used=true where t.userId=?1",u.id);
  byte[] random=new byte[32];new SecureRandom().nextBytes(random);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
  var t=new ResetToken();t.userId=u.id;t.tokenHash=hash(raw);t.expiresAt=clock.instant().plusSeconds(1800);t.used=false;db.save(t);
  String link=publicUrl+"/reset?token="+raw;
  if(!mailHost.isBlank()){var msg=new SimpleMailMessage();msg.setFrom(mailFrom);msg.setTo(u.email);msg.setSubject("Recuperar acceso · Imperio Gym");msg.setText("Enlace de un solo uso, válido durante 30 minutos: "+link);mail.getObject().send(msg);return null;}
  return development?link:null;
 }
 public void reset(String raw,String password,String confirmation){
  if(password==null||password.length()<10||password.length()>72||!password.equals(confirmation))throw new IllegalArgumentException("Usa de 10 a 72 caracteres y confirma la misma contraseña.");
  var found=db.one(ResetToken.class,"select t from ResetToken t where t.tokenHash=?1",hash(raw)).orElseThrow(()->new IllegalArgumentException("Enlace inválido o vencido."));
  var u=db.lock(UserAccount.class,found.userId);var t=db.lock(ResetToken.class,found.id);db.em().refresh(t);
  if(t.used||!t.expiresAt.isAfter(clock.instant()))throw new IllegalArgumentException("Enlace inválido o vencido.");
  if(password.equals(u.dni))throw new IllegalArgumentException("No uses tu DNI como contraseña.");
  u.passwordHash=encoder.encode(password);db.execute("update ResetToken t set t.used=true where t.userId=?1",u.id);
 }
 static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
