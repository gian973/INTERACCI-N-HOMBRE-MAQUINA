package pe.imperio.gym.service;
import java.time.LocalDate;
/** Implementations must never log phone numbers, tokens or message contents. */
public interface WhatsAppProvider {
 String send(String phone,String name,LocalDate expiry,int days,String deliveryKey);
}
