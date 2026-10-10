package pe.imperio.gym.web;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.math.BigDecimal;
public class Forms {
 public record Registration(@NotBlank @Size(max=100) String name,@NotBlank @Pattern(regexp="\\d{8}",message="El DNI debe tener 8 dígitos") String dni,
  @NotBlank @Email @Size(max=160) String email,@NotBlank @Size(max=20) @Pattern(regexp="\\+?(?:[0-9] ?){8,14}[0-9]",message="Ingresa un teléfono de 9 a 15 dígitos") String phone,
  @NotBlank @Size(min=10,max=72,message="Usa entre 10 y 72 caracteres") String password,@NotBlank String confirmation){}
 public record Freeze(@NotBlank @Pattern(regexp="HEALTH|TRAVEL") String kind,@NotNull LocalDate startsOn,
  @NotNull LocalDate endsOn,@NotBlank @Size(max=1000) String comment){}
 public record Preferences(@NotBlank @Size(max=20) @Pattern(regexp="\\+?(?:[0-9] ?){8,14}[0-9]",message="Ingresa un número válido con código de país") String phone,boolean consent,boolean notice7,boolean notice1){}
 public record PlanEdit(@NotBlank @Size(max=100) String name,@Min(1) @Max(36) int months,@NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal price,boolean active){}
}
