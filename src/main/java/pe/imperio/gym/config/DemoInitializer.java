package pe.imperio.gym.config;
import pe.imperio.gym.service.DemoService;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
@Configuration @Profile({"dev","test"}) public class DemoInitializer {
 @Bean ApplicationRunner demo(DemoService demo){return args->demo.seed();}
}
