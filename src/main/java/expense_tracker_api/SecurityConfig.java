package expense_tracker_api;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/expenses/**").hasRole("EXPENSE-OWNER"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Usuarios de prueba en memoria. En una app real se guardarían en la base de datos.
    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        User.UserBuilder users = User.builder();
        UserDetails lucas = users
                .username("lucas")
                .password(passwordEncoder.encode("abc123"))
                .roles("EXPENSE-OWNER")
                .build();
        UserDetails noOwner = users
                .username("noowner")
                .password(passwordEncoder.encode("qrs456"))
                .roles("NON-OWNER")
                .build();
        UserDetails ana = users
                .username("ana")
                .password(passwordEncoder.encode("xyz789"))
                .roles("EXPENSE-OWNER")
                .build();
        return new InMemoryUserDetailsManager(lucas, noOwner, ana);
    }
}
