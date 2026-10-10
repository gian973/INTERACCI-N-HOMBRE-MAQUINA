package pe.imperio.gym.service;
import pe.imperio.gym.domain.*;
import pe.imperio.gym.data.Store;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
@Service
public class FileService {
 private final Store db; private final Path root;
 public FileService(Store db,@Value("${app.uploads}") String root){this.db=db;this.root=Path.of(root).toAbsolutePath().normalize();}
 public PrivateFile save(MultipartFile upload,Long owner,Long payment,Long request){
  if(upload==null||upload.isEmpty())throw new IllegalArgumentException("Adjunta el comprobante o documento requerido.");
  if(upload.getSize()>5242880)throw new IllegalArgumentException("El archivo debe pesar como máximo 5 MB.");
  try{
   byte[] bytes=upload.getBytes();String type;
   if(bytes.length>8&&bytes[0]=='%'&&bytes[1]=='P'&&bytes[2]=='D'&&bytes[3]=='F'&&bytes[4]=='-') type="application/pdf";
   else if(bytes.length>8&&((bytes[0]==(byte)0x89&&bytes[1]=='P'&&bytes[2]=='N'&&bytes[3]=='G')||(bytes[0]==(byte)0xff&&bytes[1]==(byte)0xd8))&&ImageIO.read(new ByteArrayInputStream(bytes))!=null)type=bytes[0]==(byte)0x89?"image/png":"image/jpeg";
   else throw new IllegalArgumentException("Solo se permiten PDF, PNG o JPEG válidos.");
   if(!type.equals(upload.getContentType()))throw new IllegalArgumentException("El tipo declarado no coincide con el contenido.");
   Files.createDirectories(root);String key=UUID.randomUUID().toString();Path path=root.resolve(key);Files.write(path,bytes,StandardOpenOption.CREATE_NEW);
   if(TransactionSynchronizationManager.isSynchronizationActive())TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){public void afterCompletion(int status){if(status!=STATUS_COMMITTED)try{Files.deleteIfExists(path);}catch(IOException ignored){}}});
   var f=new PrivateFile();f.ownerId=owner;f.paymentId=payment;f.requestId=request;f.storageKey=key;f.originalName=type.equals("application/pdf")?"documento.pdf":type.equals("image/png")?"documento.png":"documento.jpg";f.mediaType=type;f.bytes=bytes.length;return db.save(f);
  }catch(IOException ex){throw new IllegalArgumentException("No se pudo guardar el archivo.");}
 }
 public Path path(PrivateFile f){Path p=root.resolve(f.storageKey).normalize();if(!p.startsWith(root))throw new IllegalArgumentException("Archivo inválido");return p;}
}
