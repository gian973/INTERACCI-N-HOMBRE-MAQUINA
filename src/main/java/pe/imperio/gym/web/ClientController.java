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
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;
import java.util.Objects;
import java.time.temporal.ChronoUnit;
@Controller public class ClientController {
 private final Store db;private final IdentityService identity;private final GymService gym;private final PlanRepository plans;
 public ClientController(Store db,IdentityService identity,GymService gym,PlanRepository plans){this.db=db;this.identity=identity;this.gym=gym;this.plans=plans;}
 @GetMapping("/checkout") String checkout(Authentication a,HttpSession s,Model m){
  Long plan=(Long)s.getAttribute("selectedPlan");if(plan==null)return "redirect:/#planes";
  var draft=(GymService.Draft)s.getAttribute("draft");if(draft==null){draft=gym.draft(plan);s.setAttribute("draft",draft);}
  checkoutModel(identity.current(a).id,draft,m);return "checkout";
 }
 private void checkoutModel(Long user,GymService.Draft draft,Model m){
  m.addAttribute("draft",draft);var branches=gym.branches();m.addAttribute("branches",branches);
  if(!m.containsAttribute("branchId"))m.addAttribute("branchId",gym.membership(user).map(member->member.branchId).orElse(null));
  Object branchId=m.asMap().get("branchId");
  m.addAttribute("selectedBranchName",branches.stream().filter(branch->Objects.equals(branch.id,branchId)).map(branch->branch.name).findFirst().orElse("Selecciona una sede"));
  var from=gym.proposedFrom(user);m.addAttribute("from",from);m.addAttribute("until",GymService.addMonths(from,draft.months()));
 }
 @PostMapping("/checkout") String pay(Authentication a,HttpSession s,@RequestParam String key,@RequestParam String method,@RequestParam(defaultValue="") String operation,
  @RequestParam(defaultValue="false") boolean accepted,@RequestParam(required=false) MultipartFile proof,@RequestParam(required=false) Long branchId,Model m){
  var draft=(GymService.Draft)s.getAttribute("draft");
  if(draft==null||!draft.key().equals(key))throw new IllegalArgumentException("La sesión de contratación cambió. Elige de nuevo el plan.");
  Long user=identity.current(a).id;
  try{if(!accepted)throw new IllegalArgumentException("Confirma que revisaste el resumen y el método de pago.");
   var p=gym.pay(user,branchId,draft,method,operation,proof);s.removeAttribute("selectedPlan");return "redirect:/account/payments/"+p.id;
  }catch(IllegalArgumentException e){m.addAttribute("message",e.getMessage());m.addAttribute("operation",operation);m.addAttribute("method",method);m.addAttribute("branchId",branchId);checkoutModel(user,draft,m);return "checkout";}
 }
 @GetMapping("/account") String account(Authentication a,Model m){Long user=identity.current(a).id;var member=gym.membership(user).orElse(null);m.addAttribute("membership",member);
  if(member!=null){m.addAttribute("contract",db.get(Payment.class,member.paymentId));m.addAttribute("days",Math.max(0,ChronoUnit.DAYS.between(gym.today(),member.expiresOn)));}
  m.addAttribute("payments",db.list(Payment.class,"select p from Payment p where p.userId=?1 order by p.createdAt desc",user).stream().limit(5).toList());return "account";
 }
 @GetMapping("/account/renew") String renew(Model m,Authentication a){m.addAttribute("plans",plans.findByActiveTrueOrderByMonthsAsc());m.addAttribute("from",gym.proposedFrom(identity.current(a).id));return "renew";}
 @GetMapping("/account/payments") String payments(Authentication a,@RequestParam(defaultValue="0") int page,Model m){page=Math.max(0,page);Long user=identity.current(a).id;
  var all=db.list(Payment.class,"select p from Payment p where p.userId=?1 order by p.createdAt desc",user);m.addAttribute("payments",all.stream().skip((long)page*10).limit(10).toList());m.addAttribute("page",page);m.addAttribute("hasNext",all.size()>(page+1)*10);return "payments";}
 @GetMapping("/account/payments/{id}") String payment(@PathVariable Long id,Authentication a,Model m){var p=db.get(Payment.class,id);gym.owned(p.userId,identity.current(a).id);paymentModel(p,m);return "payment";}
 public void paymentModel(Payment p,Model m){m.addAttribute("payment",p);m.addAttribute("reviews",db.list(Review.class,"select r from Review r where r.paymentId=?1 order by r.createdAt",p.id));m.addAttribute("file",db.one(PrivateFile.class,"select f from PrivateFile f where f.paymentId=?1",p.id).orElse(null));m.addAttribute("benefits",db.list(ContractBenefit.class,"select b from ContractBenefit b where b.paymentId=?1",p.id));}
 @GetMapping("/account/requests") String requests(Authentication a,Model m){m.addAttribute("form",new Forms.Freeze("HEALTH",null,null,""));requestModel(identity.current(a).id,m);return "requests";}
 private void requestModel(Long user,Model m){m.addAttribute("requests",db.list(FreezeRequest.class,"select r from FreezeRequest r where r.userId=?1 order by r.createdAt desc",user));m.addAttribute("today",gym.today());}
 @PostMapping("/account/requests") String request(@Valid @ModelAttribute("form") Forms.Freeze f,BindingResult errors,@RequestParam(required=false) MultipartFile attachment,Authentication a,Model m,RedirectAttributes flash){
  Long user=identity.current(a).id;if(!errors.hasErrors())try{var r=gym.request(user,f,attachment);flash.addFlashAttribute("success","Solicitud enviada. Un administrador la revisará.");return "redirect:/account/requests/"+r.id;}catch(IllegalArgumentException e){errors.reject("request",e.getMessage());}
  requestModel(user,m);return "requests";
 }
 @GetMapping("/account/requests/{id}") String requestDetail(@PathVariable Long id,Authentication a,Model m){var r=db.get(FreezeRequest.class,id);gym.owned(r.userId,identity.current(a).id);requestDetailModel(r,m);return "request-detail";}
 public void requestDetailModel(FreezeRequest r,Model m){m.addAttribute("request",r);m.addAttribute("reviews",db.list(Review.class,"select v from Review v where v.requestId=?1 order by v.createdAt",r.id));m.addAttribute("file",db.one(PrivateFile.class,"select f from PrivateFile f where f.requestId=?1",r.id).orElse(null));}
 @GetMapping("/account/preferences") String prefs(Authentication a,Model m){var p=db.one(Preference.class,"select p from Preference p where p.userId=?1",identity.current(a).id).orElseThrow();m.addAttribute("form",new Forms.Preferences(p.phone,p.consent,p.notice7,p.notice1));return "preferences";}
 @PostMapping("/account/preferences") String prefs(@Valid @ModelAttribute("form") Forms.Preferences f,BindingResult errors,Authentication a,RedirectAttributes flash){
  if(errors.hasErrors())return "preferences";gym.preferences(identity.current(a).id,f);flash.addFlashAttribute("success","Preferencias guardadas. Los avisos se actualizaron.");return "redirect:/account/preferences";
 }
}
