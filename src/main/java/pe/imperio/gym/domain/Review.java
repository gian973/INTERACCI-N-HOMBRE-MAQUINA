package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
@Entity @Table(name="reviews")
public class Review {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="payment_id") public Long paymentId;
 public Long getPaymentId(){return paymentId;}
 @Column(name="request_id") public Long requestId;
 public Long getRequestId(){return requestId;}
 @Column(name="reviewer_id") public Long reviewerId;
 public Long getReviewerId(){return reviewerId;}
 @Column(name="from_status") public String fromStatus;
 public String getFromStatus(){return fromStatus;}
 @Column(name="to_status") public String toStatus;
 public String getToStatus(){return toStatus;}
 @Column(name="comment") public String comment;
 public String getComment(){return comment;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
}
