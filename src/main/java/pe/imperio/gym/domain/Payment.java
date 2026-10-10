package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.time.*;
import java.math.BigDecimal;
@Entity @Table(name="payments")
public class Payment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="user_id") public Long userId;
 public Long getUserId(){return userId;}
 @Column(name="plan_id") public Long planId;
 public Long getPlanId(){return planId;}
 @Column(name="plan_name") public String planName;
 public String getPlanName(){return planName;}
 @Column(name="branch_id") public Long branchId;
 public Long getBranchId(){return branchId;}
 @Column(name="branch_name") public String branchName;
 public String getBranchName(){return branchName;}
 public String getBranchLabel(){return branchName==null?"Sin sede registrada":branchName;}
 @Column(name="months") public int months;
 public int getMonths(){return months;}
 @Column(name="amount", precision=10, scale=2) public BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 @Column(name="method") public String method;
 public String getMethod(){return method;}
 @Column(name="status") public String status;
 public String getStatus(){return status;}
 @Column(name="operation_number") public String operationNumber;
 public String getOperationNumber(){return operationNumber;}
 @Column(name="idempotency_key") public String idempotencyKey;
 public String getIdempotencyKey(){return idempotencyKey;}
 @Column(name="created_at") public Instant createdAt;
 public Instant getCreatedAt(){return createdAt;}
 @Column(name="applied_from") public LocalDate appliedFrom;
 public LocalDate getAppliedFrom(){return appliedFrom;}
 public LocalDate getAppliedUntil(){return appliedFrom==null?null:appliedFrom.plusMonths(months);}
}
