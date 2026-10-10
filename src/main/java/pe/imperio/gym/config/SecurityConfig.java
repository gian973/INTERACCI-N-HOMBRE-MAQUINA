package pe.imperio.gym.config;
import pe.imperio.gym.data.*;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService users(UserRepository users,Store db){return login->{
  var u=users.findByEmailIgnoreCaseOrDni(login.strip(),login.strip()).orElseThrow(()->new UsernameNotFoundException("Credenciales inválidas"));
  var roles=db.list(String.class,"select r.name from RoleEntry r, UserRoleEntry ur where ur.roleId=r.id and ur.userId=?1",u.id);
  return User.withUsername(u.email).password(u.passwordHash).roles(roles.toArray(String[]::new)).build();
 };}
 @Bean SecurityFilterChain web(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/","/css/**","/js/**","/images/**","/login","/register","/forgot","/reset","/choose/**","/error").permitAll()
    .requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/account/**","/checkout/**").hasRole("CLIENTE").anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").usernameParameter("login").successHandler((req,res,auth)->{
    boolean admin=auth.getAuthorities().stream().anyMatch(x->x.getAuthority().equals("ROLE_ADMIN"));
    res.sendRedirect(admin?"/admin":req.getSession().getAttribute("selectedPlan")!=null?"/checkout":"/account");
   }).failureUrl("/login?error"))
   .logout(l->l.logoutSuccessUrl("/?logout").invalidateHttpSession(true).deleteCookies("JSESSIONID"))
   .headers(h->h.contentSecurityPolicy(c->c.policyDirectives("default-src 'self'; img-src 'self' data:; style-src 'self' https://fonts.googleapis.com; font-src 'self' https://fonts.gstatic.com; script-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"))
    .referrerPolicy(r->r.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)));
  return http.build();
 }
}
