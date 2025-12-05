// src/main/java/ca/dtadmi/gamehubapi/security/JwtTokenProvider.java
package ca.dtadmi.gamehubapi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs}")
    private int jwtExpirationInMs;

    private byte[] signingKey() {
        // jjwt 0.11.x expects base64-encoded secret when passing String. Decode explicitly for clarity.
        return Decoders.BASE64.decode(jwtSecret);
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, signingKey())
                .compact();
    }

    public int getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(signingKey())
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().setSigningKey(signingKey()).parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.io.DecodingException ex) {
            // token or secret not base64/invalid structure
        } catch (SignatureException ex) {
            // invalid signature
        } catch (MalformedJwtException ex) {
            // malformed token
        } catch (ExpiredJwtException ex) {
            // expired token
        } catch (UnsupportedJwtException ex) {
            // unsupported token
        } catch (IllegalArgumentException ex) {
            // empty token
        }
        return false;
    }
}
