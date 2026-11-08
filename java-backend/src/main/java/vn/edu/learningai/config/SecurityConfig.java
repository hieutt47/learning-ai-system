package vn.edu.learningai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import vn.edu.learningai.security.custom.CustomAuthenticationEntryPoint;
import vn.edu.learningai.security.custom.CustomAccessDeniedHandler;
import vn.edu.learningai.security.custom.CustomUserDetailsService;
import vn.edu.learningai.security.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService userDetailsService;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) {
        return httpSecurity
            .getSharedObject(AuthenticationManagerBuilder.class)
            .authenticationProvider(authenticationProvider())
            .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        httpSecurity
            .csrf((csrfConfigurer -> csrfConfigurer.disable()))
            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.POST,
                    "/api/v1/auth/register", "/api/v1/auth/login",
                    "/api/v1/auth/refresh", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/courses", "/api/v1/courses/*").permitAll()


                .requestMatchers(HttpMethod.GET,
                    "/api/v1/users/me", "/api/v1/teacher-requests/me").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/me").authenticated()


                .requestMatchers(HttpMethod.GET,
                    "/api/v1/users", "/api/v1/teacher-requests",
                    "/api/v1/statistics/system").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,
                    "/api/v1/users/*/status", "/api/v1/teacher-requests/*/decision").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/categories").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/categories/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*").hasRole("ADMIN")


                .requestMatchers(HttpMethod.POST,
                    "/api/v1/teacher-requests", "/api/v1/courses/*/enroll",
                    "/api/v1/quizzes/*/attempts", "/api/v1/attempts/*/submit").hasRole("STUDENT")
                .requestMatchers(HttpMethod.GET,
                    "/api/v1/users/me/courses", "/api/v1/users/me/attempts").hasRole("STUDENT")


                .requestMatchers(HttpMethod.GET,
                    "/api/v1/teaching/courses", "/api/v1/teaching/courses/*",
                    "/api/v1/courses/*/members", "/api/v1/statistics/courses/*",
                    "/api/v1/statistics/quizzes/*").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/courses", "/api/v1/courses/*/thumbnail",
                    "/api/v1/courses/*/lessons", "/api/v1/lessons/*/documents",
                    "/api/v1/lessons/*/quizzes", "/api/v1/quizzes/*/questions",
                    "/api/v1/lessons/*/flashcards",
                    "/api/v1/ai/generate-quiz", "/api/v1/ai/generate-flashcards")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,
                    "/api/v1/courses/*", "/api/v1/courses/*/publication", "/api/v1/courses/*/archive",
                    "/api/v1/lessons/*", "/api/v1/quizzes/*", "/api/v1/quizzes/*/publication",
                    "/api/v1/flashcards/*", "/api/v1/flashcards/*/publication")
                    .hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/questions/*").hasAnyRole("TEACHER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE,
                    "/api/v1/courses/*", "/api/v1/lessons/*", "/api/v1/documents/*",
                    "/api/v1/quizzes/*", "/api/v1/questions/*", "/api/v1/flashcards/*")
                    .hasAnyRole("TEACHER", "ADMIN")


                .requestMatchers(HttpMethod.GET,
                    "/api/v1/courses/*/lessons", "/api/v1/lessons/*",
                    "/api/v1/lessons/*/documents", "/api/v1/documents/*/content",
                    "/api/v1/lessons/*/quizzes", "/api/v1/quizzes/*",
                    "/api/v1/lessons/*/flashcards", "/api/v1/attempts/*").authenticated()


                .anyRequest().denyAll())
            .sessionManagement(session -> session.sessionCreationPolicy((SessionCreationPolicy.STATELESS)))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return httpSecurity.build();
    }
}
