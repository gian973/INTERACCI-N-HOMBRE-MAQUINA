package pe.imperio.gym.web;
import pe.imperio.gym.service.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @Profile("dev") public class DevController {
 private final DemoService demo;private final IdentityService identity;
 public DevController(DemoService demo,IdentityService identity){this.demo=demo;this.identity=identity;}
 @PostMapping("/admin/dev/reset") String reset(@RequestParam String confirmation,Authentication a,RedirectAttributes flash){
  if(!confirmation.equals("REINICIAR DEMO"))throw new IllegalArgumentException("Escribe REINICIAR DEMO para confirmar.");
  demo.reset(identity.current(a).id);flash.addFlashAttribute("success","Datos de las cuentas de demostración reiniciados.");return "redirect:/admin";
 }
}
