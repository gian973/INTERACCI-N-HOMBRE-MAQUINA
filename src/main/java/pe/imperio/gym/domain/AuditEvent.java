package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="audit_events")
public class AuditEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="actor_id") public Long actorId;
 public Long getActorId(){return actorId;}
 @Column(name="action") public String action;
 public String getAction(){return action;}
 @Column(name="target_type") public String targetType;
 public String getTargetType(){return targetType;}
 @Column(name="target_id") public Long targetId;
 public Long getTargetId(){return targetId;}
 @Column(name="detail") public String detail;
 public String getDetail(){return detail;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
}
