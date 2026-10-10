package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="freeze_requests")
public class FreezeRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="kind") public String kind;
 public String getKind(){return kind;}
 @Column(name="starts_on") public LocalDate startsOn;
 public LocalDate getStartsOn(){return startsOn;}
 @Column(name="ends_on") public LocalDate endsOn;
 public LocalDate getEndsOn(){return endsOn;}
 @Column(name="comment") public String comment;
 public String getComment(){return comment;}
 @Column(name="status") public String status;
 public String getStatus(){return status;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
 @Column(name="policy") public String policy;
 public String getPolicy(){return policy;}
 public int getExtensionDays(){return "EXTEND_DAYS".equals(policy)?(int)java.time.temporal.ChronoUnit.DAYS.between(startsOn,endsOn)+1:0;}
}
