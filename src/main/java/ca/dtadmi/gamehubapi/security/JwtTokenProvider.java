// src/main/java/ca/dtadmi/gamehubapi/security/JwtTokenProvider.java
package ca.dtadmi.gamehubapi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${app.jwtSecret:dev-secret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationInMs:86400000}")
    private int jwtExpirationInMs;

    private byte[] signingKey() {
        // Accept both raw text and base64-encoded secrets. If resulting key < 32 bytes,
        // derive a stable 256-bit key via SHA-256 to satisfy HS256 minimum.
        byte[] raw;
        try {
            raw = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception ignored) {
            raw = jwtSecret.getBytes();
        }
        if (raw.length >= 32) return raw;
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            return sha256.digest(raw);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: pad/repeat to 32 bytes (unlikely path)
            byte[] padded = new byte[32];
            for (int i = 0; i < 32; i++) padded[i] = raw[i % raw.length];
            return padded;
        }
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(signingKey());
    }

    public String generateToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        // After derivation above, key is guaranteed >= 32 bytes; sign with HS256 by default.
        SecretKey key = key();
        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a JWT where the subject is explicitly provided (e.g., email).
     * Useful to ensure the token subject is the stable login identifier
     * even when UserDetails#getUsername() returns a display username.
     */
    public String generateTokenForSubject(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        SecretKey key = key();
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public int getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(authToken);
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
