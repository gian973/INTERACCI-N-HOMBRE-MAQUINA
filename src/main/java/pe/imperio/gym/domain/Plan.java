package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="plans")
public class Plan {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="name") public String name;
 public String getName(){return name;}
 @Column(name="months") public int months;
 public int getMonths(){return months;}
 @Column(name="price", precision=10, scale=2) public BigDecimal price;
 public BigDecimal getPrice(){return price;}
 @Column(name="active") public boolean active;
 public boolean getActive(){return active;}
}
