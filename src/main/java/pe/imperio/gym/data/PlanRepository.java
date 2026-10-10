package pe.imperio.gym.data;
import pe.imperio.gym.domain.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PlanRepository extends JpaRepository<Plan,Long> { List<Plan> findByActiveTrueOrderByMonthsAsc(); }
