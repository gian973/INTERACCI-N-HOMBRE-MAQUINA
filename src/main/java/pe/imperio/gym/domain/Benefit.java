package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="benefits")
public class Benefit {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="label") public String label;
 public String getLabel(){return label;}
}
