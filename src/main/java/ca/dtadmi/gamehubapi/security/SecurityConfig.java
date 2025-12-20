package ca.dtadmi.gamehubapi.security;

import com.google.firebase.auth.FirebaseAuth;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Duration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    @Value("${app.jwtSecret:dev-secret}")
    private String jwtSecret;

    // Control Swagger visibility: open in dev by default; set APP_SWAGGER_OPEN=false in prod to require auth
    @Value("${app.swagger.open:true}")
    private boolean swaggerOpen;

    // Rate limits per minute (configurable via env): USER_RPM, GUEST_RPM
    @Value("${app.ratelimit.userRpm:300}")
    private int userRpm;
    @Value("${app.ratelimit.guestRpm:60}")
    private int guestRpm;

    public SecurityConfig(JwtAuthenticationEntryPoint unauthorizedHandler) {
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider tokenProvider, CustomUserDetailsService uds) {
        return new JwtAuthenticationFilter(tokenProvider, uds);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthFilter,
                                           org.springframework.beans.factory.ObjectProvider<FirebaseAuth> firebaseAuthProvider) throws Exception {
        http
                .cors(cors -> {
                })
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> {
                    // Base public routes
                    var config = authz
                            .requestMatchers(
                                    "/api/auth/**",
                                    "/graphql",
                                    "/api/health",
                                    "/api/leaderboard",
                                    "/api/scores",
                                    "/api/scores/**",
                                    "/api/stats/**",
                                    "/healthz",
                                    "/favicon.ico",
                                    "/",
                                    "/api/featured",
                                    "/api/projects/**",
                                    "/api/features",
                                    "/api/meta",
                                    "/actuator/health",
                                    "/actuator/info"
                            ).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // Swagger/UI: permitAll only when explicitly open; otherwise require auth
                    if (swaggerOpen) {
                        config = config
                                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    }

                    config.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; connect-src 'self' ws: wss:; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self' data:"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(rp -> rp.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).preload(true))
                        .contentTypeOptions(withDefaults -> {
                        })
                );

        // If Firebase is configured, add FirebaseTokenFilter before UsernamePasswordAuthenticationFilter
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth != null) {
            http.addFilterBefore(new FirebaseTokenFilter(firebaseAuth), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Registry can hold multiple independent limiters
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean
    public RateLimiter userRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("api-user",
                RateLimiterConfig.custom()
                        .limitForPeriod(Math.max(1, userRpm))
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build());
    }

    @Bean
    public RateLimiter guestRateLimiter(RateLimiterRegistry registry) {
        return registry.rateLimiter("api-guest",
                RateLimiterConfig.custom()
                        .limitForPeriod(Math.max(1, guestRpm))
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .timeoutDuration(Duration.ofSeconds(2))
                        .build());
    }

    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilter(RateLimiter userRateLimiter, RateLimiter guestRateLimiter) {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RateLimitFilter(userRateLimiter, guestRateLimiter));
        registrationBean.addUrlPatterns("/api/*");
        return registrationBean;
    }

    // CORS is configured in WebConfig class
}
