package pe.imperio.gym;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.*;
@SpringBootApplication
@EnableScheduling
public class GymApplication {
 public static void main(String[] args) { SpringApplication.run(GymApplication.class,args); }
 @Bean Clock clock() { return Clock.system(ZoneId.of("America/Lima")); }
}
