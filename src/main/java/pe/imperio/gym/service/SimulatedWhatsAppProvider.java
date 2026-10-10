package pe.imperio.gym.service;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Component @ConditionalOnProperty(name="app.whatsapp.provider",havingValue="simulated",matchIfMissing=true)
public class SimulatedWhatsAppProvider implements WhatsAppProvider {
 public String send(String phone,String name,LocalDate expiry,int days,String key){return "SIMULATED";}
}
