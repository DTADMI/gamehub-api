package ca.dtadmi.gamehubapi.service;

import ca.dtadmi.gamehubapi.model.RefreshToken;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.RefreshTokenRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.refreshTokenExpirationMs:2592000000}") // 30 days default
    private long refreshExpirationMs;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public String generateTokenString() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Transactional
    public RefreshToken issue(User user) {
        // Optionally revoke existing tokens for the user (single-device policy)
        // refreshTokenRepository.deleteByUser(user);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken(generateTokenString());
        rt.setExpiresAt(Instant.now().plusMillis(refreshExpirationMs));
        rt.setRevoked(false);
        return refreshTokenRepository.save(rt);
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findValid(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(rt -> !rt.isRevoked())
                .filter(rt -> rt.getExpiresAt().isAfter(Instant.now()));
    }

    @Transactional
    public RefreshToken rotate(RefreshToken existing) {
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);
        // issue a new one for the same user
        return issue(existing.getUser());
    }

    @Transactional
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Transactional
    public void cleanupExpired() {
        refreshTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}