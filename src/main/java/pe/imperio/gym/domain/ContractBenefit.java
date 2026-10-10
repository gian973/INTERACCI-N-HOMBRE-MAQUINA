package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="contract_benefits")
public class ContractBenefit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="payment_id") public Long paymentId;
 public Long getPaymentId(){return paymentId;}
 @Column(name="label") public String label;
 public String getLabel(){return label;}
}
