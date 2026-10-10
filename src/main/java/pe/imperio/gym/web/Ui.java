package pe.imperio.gym.web;
import org.springframework.stereotype.Component;
import java.time.*;
import java.time.format.DateTimeFormatter;
import pe.imperio.gym.data.Store;
import pe.imperio.gym.domain.*;
import java.util.*;
@Component("ui") public class Ui {
 private final Store db;public Ui(Store db){this.db=db;}
 public String label(String v){if(v==null)return "—";return switch(v){
 case "PENDING"->"Pendiente de validación";case "CONFIRMED"->"Confirmado";case "REJECTED"->"Rechazada";case "SENT"->"Enviada";case "IN_REVIEW"->"En revisión";case "APPROVED"->"Aprobada";case "HEALTH"->"Salud";case "TRAVEL"->"Viaje";case "CARD_TEST"->"Tarjeta de prueba";case "WALLET"->"Yape / Plin";case "TRANSFER"->"Transferencia";case "SCHEDULED"->"Programado";case "CANCELLED"->"Cancelado";case "SIMULATED"->"Simulado";case "FAILED"->"Fallido";default->v;};}
 public String date(java.time.temporal.TemporalAccessor date){return date==null?"—":date instanceof Instant i?DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima")).format(i):DateTimeFormatter.ofPattern("dd/MM/yyyy").format(date);}
 public String user(Long id){return id==null?"Sistema":db.get(UserAccount.class,id).name;}
 public String tone(String status){return Set.of("CONFIRMED","APPROVED","SENT","SIMULATED").contains(status)?"success":Set.of("REJECTED","FAILED","CANCELLED").contains(status)?"danger":"warning";}
}
