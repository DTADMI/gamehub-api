// src/main/java/ca/dtadmi/gamehubapi/controller/AuthController.java
package ca.dtadmi.gamehubapi.controller;

import ca.dtadmi.gamehubapi.model.RefreshToken;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import ca.dtadmi.gamehubapi.security.CustomUserDetailsService;
import ca.dtadmi.gamehubapi.security.JwtTokenProvider;
import ca.dtadmi.gamehubapi.service.RefreshTokenService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.beans.factory.ObjectProvider<FirebaseAuth> firebaseAuthProvider;

    @Value("${app.jwtExpirationInMs:3600000}")
    private int jwtExpirationInMs;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            RefreshTokenService refreshTokenService,
            CustomUserDetailsService userDetailsService,
            org.springframework.beans.factory.ObjectProvider<FirebaseAuth> firebaseAuthProvider
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.firebaseAuthProvider = firebaseAuthProvider;
    }

    /**
     * Backwards-compatible alias for login using the preferred nomenclature "signin".
     * Low priority migration: clients should gradually switch to /signin; /login remains available.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUserSignin(@RequestBody(required = false) Map<String, Object> loginRequest,
                                                    jakarta.servlet.http.HttpServletRequest request) {
        return authenticateUser(loginRequest, request);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody(required = false) Map<String, Object> loginRequest,
                                              jakarta.servlet.http.HttpServletRequest request) {
        try {
            String email = firstNonBlank(loginRequest, request, "email");
            String password = firstNonBlank(loginRequest, request, "password");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            email,
                            password
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Ensure JWT subject is the user's email for consistent identity across services
            String accessToken;
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }
            accessToken = tokenProvider.generateTokenForSubject(userOpt.get().getEmail());

            // Issue refresh token
            RefreshToken rt = refreshTokenService.issue(userOpt.get());

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", rt.getToken());
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpirationInMs);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "error", "Unauthorized",
                    "message", "Bad credentials"
            ));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody(required = false) Map<String, Object> signUpRequest,
                                          jakarta.servlet.http.HttpServletRequest request) {
        String email = firstNonBlank(signUpRequest, request, "email");
        String username = firstNonBlank(signUpRequest, request, "username");
        String password = firstNonBlank(signUpRequest, request, "password");

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "error", "Bad Request",
                    "message", "Email is already taken"
            ));
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.getRoles().add("ROLE_USER");

        userRepository.save(user);

        // Auto login: issue tokens using a proper principal
        UserDetails ud = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication authentication = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        String accessToken = tokenProvider.generateTokenForSubject(user.getEmail());
        RefreshToken rt = refreshTokenService.issue(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("accessToken", accessToken);
        response.put("refreshToken", rt.getToken());
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtExpirationInMs);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody(required = false) Map<String, Object> req,
                                     jakarta.servlet.http.HttpServletRequest request) {
        String token = firstNonBlank(req, request, "refreshToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "refreshToken required"));
        }
        Optional<RefreshToken> valid = refreshTokenService.findValid(token);
        if (valid.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "invalid or expired refresh token"));
        }
        RefreshToken rotated = refreshTokenService.rotate(valid.get());
        User user = rotated.getUser();
        UserDetails ud = userDetailsService.loadUserByUsername(user.getEmail());
        Authentication auth = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
        String newAccess = tokenProvider.generateTokenForSubject(user.getEmail());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccess);
        response.put("refreshToken", rotated.getToken());
        response.put("tokenType", "Bearer");
        response.put("expiresIn", jwtExpirationInMs);
        return ResponseEntity.ok(response);
    }

    /**
     * Exchange a Firebase ID token (from frontend social login) for our API JWT + refresh token.
     * Accepts the ID token in the Authorization header (Bearer) or JSON body key "idToken".
     */
    @PostMapping("/firebase/exchange")
    public ResponseEntity<?> exchangeFirebaseToken(@RequestBody(required = false) Map<String, Object> body,
                                                   @RequestHeader(value = "Authorization", required = false) String authorization) {
        FirebaseAuth firebaseAuth = firebaseAuthProvider.getIfAvailable();
        if (firebaseAuth == null) {
            return ResponseEntity.status(501).body(Map.of(
                    "error", "Firebase not configured",
                    "message", "Enable Firebase by setting NEXT_FIREBASE_CREDS_* and running with prod profile"
            ));
        }

        String idToken = null;
        if (authorization != null && authorization.startsWith("Bearer ")) {
            idToken = authorization.substring("Bearer ".length());
        }
        if ((idToken == null || idToken.isBlank()) && body != null && body.get("idToken") != null) {
            idToken = String.valueOf(body.get("idToken"));
        }
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "idToken required (Authorization: Bearer or body.idToken)"));
        }

        try {
            FirebaseToken decoded = firebaseAuth.verifyIdToken(idToken);
            String email = decoded.getEmail();
            String name = decoded.getName();
            if (email == null || email.isBlank()) {
                // fallback to UID-based synthetic email
                email = decoded.getUid() + "@firebase.local";
            }
            final String fEmail = email;
            final String fName = name;

            // Upsert user
            User user = userRepository.findByEmail(fEmail).orElseGet(() -> {
                User u = new User();
                u.setEmail(fEmail);
                u.setUsername((fName != null && !fName.isBlank()) ? fName : fEmail);
                u.setPassword("{noop}firebase");
                u.getRoles().add("ROLE_USER");
                return u;
            });
            if (user.getId() == null) {
                user = userRepository.save(user);
            }

            String accessToken = tokenProvider.generateTokenForSubject(user.getEmail());
            RefreshToken rt = refreshTokenService.issue(user);

            Map<String, Object> response = new HashMap<>();
            response.put("accessToken", accessToken);
            response.put("refreshToken", rt.getToken());
            response.put("tokenType", "Bearer");
            response.put("expiresIn", jwtExpirationInMs);
            response.put("provider", decoded.getIssuer());
            response.put("uid", decoded.getUid());

            return ResponseEntity.ok(response);
        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "invalid_firebase_token",
                    "message", e.getMessage()
            ));
        }
    }

    private String firstNonBlank(Map<String, Object> body, jakarta.servlet.http.HttpServletRequest request, String key) {
        String fromBody = null;
        if (body != null && body.containsKey(key)) {
            Object v = body.get(key);
            if (v != null) {
                fromBody = String.valueOf(v);
            }
        }
        if (fromBody != null && !fromBody.isBlank()) return fromBody;
        String fromParam = request.getParameter(key);
        return (fromParam == null || fromParam.isBlank()) ? null : fromParam;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetails principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        // Prefer email when our principal type exposes it; otherwise fall back to username
        String email;
        if (principal instanceof ca.dtadmi.gamehubapi.security.UserPrincipal up && up.getEmail() != null) {
            email = up.getEmail();
        } else {
            email = principal.getUsername();
        }
        Optional<User> userOpt = userRepository.findByEmail(email);
        // If the user is not found in DB (e.g., test fallback principal), still return principal identity
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of("email", email));
        }
        User u = userOpt.get();
        Map<String, Object> body = new HashMap<>();
        body.put("id", u.getId());
        body.put("email", u.getEmail());
        body.put("username", u.getUsername());
        body.put("roles", u.getRoles());
        return ResponseEntity.ok(body);
    }
}

class LoginRequest {
    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

class SignUpRequest {
    @NotBlank
    @Size(min = 2, max = 100)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 4, max = 200)
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

class RefreshRequest {
    @NotBlank
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
