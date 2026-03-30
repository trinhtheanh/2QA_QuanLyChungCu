//package phattrienungdungvoij2ee.bai5_qlsp_jpa.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.web.SecurityFilterChain;
//
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
//
//    @Bean
//    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
//            throws Exception {
//        return authConfig.getAuthenticationManager();
//    }
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//        http
//                .authorizeHttpRequests(authorize -> authorize
//                        .requestMatchers("/login", "/register", "/debug", "/css/**", "/js/**").permitAll()
//                        .requestMatchers("/thongbao/xem").hasAnyRole("USER", "ADMIN", "MANAGER")
//                        .requestMatchers("/thongbao", "/thongbao/**").hasRole("MANAGER")
//                        .requestMatchers("/tintuc", "/tintuc/**").hasAnyRole("USER", "ADMIN", "MANAGER")
//                        .requestMatchers("/Apartments").hasAnyRole("USER", "ADMIN", "MANAGER")
//                        .requestMatchers("/Apartments/**", "/categories/**", "/services/**", "/service-categories/**").hasRole("ADMIN")
//                        .anyRequest().authenticated()
//                )
//                // Cho phép form login nội bộ
//                .formLogin(form -> form
//                        .loginPage("/login")
//                        .loginProcessingUrl("/do-login")
//                        .defaultSuccessUrl("/Apartments", true)
//                        .failureUrl("/login?error=true")
//                        .permitAll()
//                )
//                // Thêm OAuth2 login (Google)
//                .oauth2Login(oauth2 -> oauth2
//                        .loginPage("/login") // dùng chung trang login
//                        .defaultSuccessUrl("/Apartments", true)
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/logout")
//                        .logoutSuccessUrl("/login?logout=true")
//                        .permitAll()
//                )
//                .csrf(csrf -> csrf.ignoringRequestMatchers("/register"));
//
//        return http.build();
//    }
//}


package phattrienungdungvoij2ee.bai5_qlsp_jpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    public SecurityConfig(OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) {
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/register", "/debug", "/css/**", "/js/**", "/uploads/**", "/images/**", "/qr/**").permitAll()
                        .requestMatchers("/thongbao/xem").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/thongbao", "/thongbao/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/tintuc", "/tintuc/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/Apartments").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/Apartments/detail/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/Apartments/**", "/categories/**", "/services/**", "/service-categories/**").hasRole("ADMIN")
                        .requestMatchers("/quan-ly-dich-vu/**").hasRole("ADMIN")
                        .requestMatchers("/dich-vu/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/hoa-don/admin/**", "/hoa-don/admin").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/hoa-don/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .anyRequest().authenticated()
                )
                // Cho phép form login nội bộ
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/do-login")
                        .defaultSuccessUrl("/Apartments", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                // Thêm OAuth2 login (Google) - dùng custom handler để tạo account + gán ROLE_USER
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers("/register", "/quan-ly-dich-vu/**", "/dich-vu/**", "/hoa-don/**"));

        return http.build();
    }
}
