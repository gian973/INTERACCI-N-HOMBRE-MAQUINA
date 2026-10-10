package pe.imperio.gym.web;
import pe.imperio.gym.data.Store;
import pe.imperio.gym.domain.PrivateFile;
import pe.imperio.gym.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
@Controller public class FileController {
 private final Store db;private final FileService files;private final IdentityService identity;private final GymService gym;
 public FileController(Store db,FileService files,IdentityService identity,GymService gym){this.db=db;this.files=files;this.identity=identity;this.gym=gym;}
 @GetMapping("/files/{id}") public ResponseEntity<FileSystemResource> file(@PathVariable Long id,Authentication auth){
  var f=db.get(PrivateFile.class,id);if(auth.getAuthorities().stream().noneMatch(a->a.getAuthority().equals("ROLE_ADMIN")))gym.owned(f.ownerId,identity.current(auth).id);
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+f.originalName+"\"").header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(f.mediaType)).body(new FileSystemResource(files.path(f)));
 }
}
