package pe.imperio.gym.domain;
import jakarta.persistence.*;
import java.io.Serializable;
@Entity @Table(name="user_roles") @IdClass(UserRoleEntry.Key.class)
public class UserRoleEntry {
 @Id @Column(name="user_id") public Long userId;
 @Id @Column(name="role_id") public Long roleId;
 public static class Key implements Serializable {
  public Long userId; public Long roleId;
  public Key(){}
  @Override public boolean equals(Object o){return o instanceof Key k && java.util.Objects.equals(userId,k.userId)&&java.util.Objects.equals(roleId,k.roleId);}
  @Override public int hashCode(){return java.util.Objects.hash(userId,roleId);}
 }
}
