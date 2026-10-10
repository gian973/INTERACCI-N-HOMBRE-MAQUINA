package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="reset_tokens")
public class ResetToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="token_hash") public String tokenHash;
 public String getTokenHash(){return tokenHash;}
 @Column(name="expires_at") public Instant expiresAt;
 public Instant getExpiresAt(){return expiresAt;}
 @Column(name="used") public boolean used;
 public boolean getUsed(){return used;}
}
