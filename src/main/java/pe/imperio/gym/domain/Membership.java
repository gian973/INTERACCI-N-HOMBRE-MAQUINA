package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="memberships")
public class Membership {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="payment_id") public Long paymentId;
 public Long getPaymentId(){return paymentId;}
 @Column(name="started_on") public LocalDate startedOn;
 public LocalDate getStartedOn(){return startedOn;}
 @Column(name="expires_on") public LocalDate expiresOn;
 public LocalDate getExpiresOn(){return expiresOn;}
}
