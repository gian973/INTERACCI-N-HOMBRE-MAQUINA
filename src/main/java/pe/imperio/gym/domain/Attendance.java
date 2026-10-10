package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="attendance")
public class Attendance {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="branch_id") public Long branchId;
 public Long getBranchId(){return branchId;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="checked_in_at") public Instant checkedInAt;
 public Instant getCheckedInAt(){return checkedInAt;}
 @Column(name="checked_out_at") public Instant checkedOutAt;
 public Instant getCheckedOutAt(){return checkedOutAt;}
}
