package ca.dtadmi.gamehubapi.security;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider();
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String b64 = Encoders.BASE64.encode(key.getEncoded());
        ReflectionTestUtils.setField(tokenProvider, "jwtSecret", b64);
        ReflectionTestUtils.setField(tokenProvider, "jwtExpirationInMs", 2000);

        UserDetails userDetails = User.withUsername("test@example.com")
                .password("pass")
                .authorities(Collections.emptyList())
                .build();
        authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Test
    void generateAndValidateToken_success() {
        String token = tokenProvider.generateToken(authentication);
        assertThat(token).isNotBlank();
        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getUsernameFromJWT(token)).isEqualTo("test@example.com");
    }

    @Test
    void validateToken_returnsFalseForMalformed() {
        assertThat(tokenProvider.validateToken("not-a-jwt")).isFalse();
    }
}
