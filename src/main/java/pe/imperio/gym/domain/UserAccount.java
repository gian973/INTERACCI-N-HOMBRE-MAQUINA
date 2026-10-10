package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="users")
public class UserAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="name") public String name;
 public String getName(){return name;}
 @Column(name="dni") public String dni;
 public String getDni(){return dni;}
 @Column(name="email") public String email;
 public String getEmail(){return email;}
 @Column(name="phone") public String phone;
 public String getPhone(){return phone;}
 @Column(name="password_hash") public String passwordHash;
 public String getPasswordHash(){return passwordHash;}
 @Column(name="demo") public boolean demo;
 public boolean getDemo(){return demo;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
}
