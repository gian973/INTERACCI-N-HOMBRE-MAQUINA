package pe.imperio.gym.service;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import pe.imperio.gym.data.Store;
import java.time.*;
@Component public class ReminderScheduler {
 private final Store db;private final ReminderService service;private final Clock clock;
 public ReminderScheduler(Store db,ReminderService service,Clock clock){this.db=db;this.service=service;this.clock=clock;}
 @Scheduled(cron="0 0 * * * *",zone="America/Lima") public void run(){
  for(Long id:db.list(Long.class,"select r.id from Reminder r where r.status='SCHEDULED' and ((r.daysBefore=7 and r.expiryDate<=?1) or (r.daysBefore=1 and r.expiryDate<=?2))",LocalDate.now(clock).plusDays(7),LocalDate.now(clock).plusDays(1)))service.deliver(id);
 }
}
