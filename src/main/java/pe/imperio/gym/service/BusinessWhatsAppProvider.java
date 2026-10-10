package pe.imperio.gym.service;
import java.time.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="app.whatsapp.provider",havingValue="business")
public class BusinessWhatsAppProvider implements WhatsAppProvider {
 private final String token,phoneId,template,version;
 public BusinessWhatsAppProvider(@Value("${app.whatsapp.token}") String token,@Value("${app.whatsapp.phone-id}") String phoneId,@Value("${app.whatsapp.template}") String template,@Value("${app.whatsapp.api-version}") String version){
  if(token.isBlank()||!phoneId.matches("[0-9]+")||template.isBlank()||!version.matches("v[0-9]+\\.[0-9]+"))throw new IllegalStateException("Falta configuración de WhatsApp Business.");
  this.token=token;this.phoneId=phoneId;this.template=template;this.version=version;
 }
 public String send(String phone,String name,LocalDate expiry,int days,String key){
  try{
   var parameters=List.of(Map.of("type","text","text",name),Map.of("type","text","text",expiry.toString()),Map.of("type","text","text",Integer.toString(days)));
   var body=Map.of("messaging_product","whatsapp","to",phone.replaceAll("[^0-9]",""),"type","template","template",Map.of("name",template,"language",Map.of("code","es"),"components",List.of(Map.of("type","body","parameters",parameters))));
   var req=HttpRequest.newBuilder(URI.create("https://graph.facebook.com/"+version+"/"+phoneId+"/messages")).timeout(Duration.ofSeconds(15)).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(new ObjectMapper().writeValueAsString(body))).build();
   var response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(req,HttpResponse.BodyHandlers.discarding());return response.statusCode()/100==2?"SENT":"FAILED";
  }catch(Exception e){return "FAILED";}
 }
}
