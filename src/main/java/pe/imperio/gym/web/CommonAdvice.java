package pe.imperio.gym.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import pe.imperio.gym.service.IdentityService;
@ControllerAdvice public class CommonAdvice {
 private final IdentityService identity;@Value("${app.development:false}") boolean development;
 public CommonAdvice(IdentityService identity){this.identity=identity;}
 @ModelAttribute public void shared(Authentication auth,Model model){
  model.addAttribute("signedIn",auth!=null);model.addAttribute("isAdmin",auth!=null&&auth.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN")));model.addAttribute("development",development);
  if(auth!=null)model.addAttribute("me",identity.current(auth));
 }
 @ExceptionHandler(IllegalArgumentException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public String invalid(IllegalArgumentException e,Model m){m.addAttribute("message",e.getMessage());return "error";}
 @ExceptionHandler(DataIntegrityViolationException.class) @ResponseStatus(HttpStatus.CONFLICT) public String duplicate(Model m){m.addAttribute("message","La operación entra en conflicto con un registro existente. Actualiza tu historial antes de volver a intentarlo.");return "error";}
 @ExceptionHandler(MaxUploadSizeExceededException.class) @ResponseStatus(HttpStatus.BAD_REQUEST) public String upload(Model m){m.addAttribute("message","El archivo excede el máximo de 5 MB.");return "error";}
}
