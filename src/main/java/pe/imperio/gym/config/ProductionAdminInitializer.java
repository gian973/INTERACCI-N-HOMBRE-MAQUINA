package pe.imperio.gym.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import pe.imperio.gym.data.Store;
import pe.imperio.gym.service.IdentityService;
import pe.imperio.gym.domain.UserRoleEntry;
import pe.imperio.gym.web.Forms;
import jakarta.validation.Validator;
@Configuration @Profile("!dev & !test")
public class ProductionAdminInitializer {
 @Bean ApplicationRunner bootstrapAdmin(Store db,IdentityService identity,Validator validator,PlatformTransactionManager transactions,
  @Value("${BOOTSTRAP_ADMIN_EMAIL:}") String email,@Value("${BOOTSTRAP_ADMIN_DNI:}") String dni,@Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password){
  return args->new TransactionTemplate(transactions).executeWithoutResult(tx->{
   if(db.count("select count(r) from UserRoleEntry r where r.roleId=2")>0||email.isBlank())return;
   var form=new Forms.Registration("Administrador",dni,email,"+51000000000",password,password);
   if(!validator.validate(form).isEmpty())throw new IllegalArgumentException("Configura correctamente las variables BOOTSTRAP_ADMIN.");
   var u=identity.register(form);db.execute("delete from UserRoleEntry r where r.userId=?1",u.id);
   var role=new UserRoleEntry();role.userId=u.id;role.roleId=2L;db.save(role);
  });
 }
}
