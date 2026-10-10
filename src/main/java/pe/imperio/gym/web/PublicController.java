package pe.imperio.gym.web;
import pe.imperio.gym.data.*;
import pe.imperio.gym.service.*;
import pe.imperio.gym.domain.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
@Controller public class PublicController {
 private final PlanRepository plans;private final IdentityService identity;private final GymService gym;
 public PublicController(PlanRepository plans,IdentityService identity,GymService gym){this.plans=plans;this.identity=identity;this.gym=gym;}
 @GetMapping("/") String home(Model m){m.addAttribute("plans",plans.findByActiveTrueOrderByMonthsAsc());m.addAttribute("gym",gym);m.addAttribute("branches",gym.branches());return "home";}
 @GetMapping("/choose/{id}") String choose(@PathVariable Long id,HttpSession session){gym.draft(id);session.setAttribute("selectedPlan",id);session.removeAttribute("draft");return "redirect:/checkout";}
 @GetMapping("/login") String login(HttpSession s,Model m){selected(s,m);return "login";}
 private void selected(HttpSession s,Model m){Object id=s.getAttribute("selectedPlan");if(id!=null)m.addAttribute("selected",plans.findById((Long)id).orElse(null));}
 @GetMapping("/register") String register(HttpSession s,Model m){selected(s,m);m.addAttribute("form",new Forms.Registration("","","","","",""));return "register";}
 @PostMapping("/register") String register(@Valid @ModelAttribute("form") Forms.Registration f,BindingResult errors,HttpSession s,Model m,RedirectAttributes flash){
  if(!errors.hasErrors())try{identity.register(f);flash.addFlashAttribute("success","Cuenta creada. Ingresa con tu correo o DNI para continuar.");return "redirect:/login";}catch(IllegalArgumentException e){errors.reject("registration",e.getMessage());}
  catch(DataIntegrityViolationException e){
   if(!identity.isRegistered(f.email(),f.dni()))throw e;
   errors.reject("registration","El correo o DNI ya está registrado.");
  }
  selected(s,m);return "register";
 }
 @GetMapping("/forgot") String forgot(){return "forgot";}
 @PostMapping("/forgot") String forgot(@RequestParam String login,Model m){if(login.length()>160)throw new IllegalArgumentException("Correo o DNI inválido.");String link=identity.forgot(login);m.addAttribute("success","Si existe una cuenta, se ha preparado un enlace de recuperación.");m.addAttribute("devLink",link);return "forgot";}
 @GetMapping("/reset") String reset(@RequestParam String token,Model m){m.addAttribute("token",token);return "reset";}
 @PostMapping("/reset") String reset(@RequestParam String token,@RequestParam String password,@RequestParam String confirmation,Model m,RedirectAttributes flash){
  try{identity.reset(token,password,confirmation);flash.addFlashAttribute("success","Contraseña actualizada. Inicia sesión.");return "redirect:/login";}catch(IllegalArgumentException e){m.addAttribute("token",token);m.addAttribute("message",e.getMessage());return "reset";}
 }
}
