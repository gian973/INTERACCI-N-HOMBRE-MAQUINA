package pe.imperio.gym.domain;
import jakarta.persistence.*;
@Entity @Table(name="preferences")
public class Preference {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="phone") public String phone;
 public String getPhone(){return phone;}
 @Column(name="consent") public boolean consent;
 public boolean getConsent(){return consent;}
 @Column(name="notice7") public boolean notice7;
 public boolean getNotice7(){return notice7;}
 @Column(name="notice1") public boolean notice1;
 public boolean getNotice1(){return notice1;}
}
