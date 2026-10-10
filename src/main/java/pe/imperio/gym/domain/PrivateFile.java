package pe.imperio.gym.domain;
import jakarta.persistence.*;
@Entity @Table(name="private_files")
public class PrivateFile {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long getId(){return id;}
 @Column(name="owner_id") public Long ownerId;
 public Long getOwnerId(){return ownerId;}
 @Column(name="payment_id") public Long paymentId;
 public Long getPaymentId(){return paymentId;}
 @Column(name="request_id") public Long requestId;
 public Long getRequestId(){return requestId;}
 @Column(name="storage_key") public String storageKey;
 public String getStorageKey(){return storageKey;}
 @Column(name="original_name") public String originalName;
 public String getOriginalName(){return originalName;}
 @Column(name="media_type") public String mediaType;
 public String getMediaType(){return mediaType;}
 @Column(name="bytes") public long bytes;
 public long getBytes(){return bytes;}
}
