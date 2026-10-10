package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="reminders")
public class Reminder {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="expiry_date") public LocalDate expiryDate;
 public LocalDate getExpiryDate(){return expiryDate;}
 @Column(name="days_before") public int daysBefore;
 public int getDaysBefore(){return daysBefore;}
 public LocalDate getDueOn(){return expiryDate.minusDays(daysBefore);}
 @Column(name="status") public String status;
 public String getStatus(){return status;}
 @Column(name="processed_at") public Instant processedAt;
 public Instant getProcessedAt(){return processedAt;}
}
