package pe.imperio.gym.domain;
import jakarta.persistence.*;
@Entity @Table(name="roles") public class RoleEntry {
 @Id public Long id; public String name;
}
