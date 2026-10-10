package pe.imperio.gym.data;
import pe.imperio.gym.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long> {
 Optional<UserAccount> findByEmailIgnoreCaseOrDni(String email,String dni);
 boolean existsByEmailIgnoreCaseOrDni(String email,String dni);
}
