package ca.dtadmi.gamehubapi.service;

import ca.dtadmi.gamehubapi.model.RefreshToken;
import ca.dtadmi.gamehubapi.model.User;
import ca.dtadmi.gamehubapi.repository.RefreshTokenRepository;
import ca.dtadmi.gamehubapi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

    private RefreshTokenRepository refreshTokenRepository;
    private UserRepository userRepository;
    private RefreshTokenService service;

    @BeforeEach
    void setup() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        service = new RefreshTokenService(refreshTokenRepository, userRepository);
        // set refreshExpirationMs via reflection since it comes from @Value
        try {
            var f = RefreshTokenService.class.getDeclaredField("refreshExpirationMs");
            f.setAccessible(true);
            f.setLong(service, 3600_000L); // 1h
        } catch (Exception ignored) {
        }
    }

    @Test
    void issue_setsFutureExpiryAndNotRevoked() {
        User u = new User();
        u.setId(1L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rt = service.issue(u);
        assertThat(rt.getUser()).isEqualTo(u);
        assertThat(rt.getToken()).isNotBlank();
        assertThat(rt.isRevoked()).isFalse();
        assertThat(rt.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void findValid_filtersRevokedAndExpired() {
        RefreshToken valid = new RefreshToken();
        valid.setToken("t1");
        valid.setRevoked(false);
        valid.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("t1")).thenReturn(Optional.of(valid));

        assertThat(service.findValid("t1")).isPresent();

        RefreshToken revoked = new RefreshToken();
        revoked.setToken("t2");
        revoked.setRevoked(true);
        revoked.setExpiresAt(Instant.now().plusSeconds(60));
        when(refreshTokenRepository.findByToken("t2")).thenReturn(Optional.of(revoked));
        assertThat(service.findValid("t2")).isEmpty();

        RefreshToken expired = new RefreshToken();
        expired.setToken("t3");
        expired.setRevoked(false);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByToken("t3")).thenReturn(Optional.of(expired));
        assertThat(service.findValid("t3")).isEmpty();
    }

    @Test
    void rotate_revokesOldAndIssuesNew() {
        User u = new User();
        u.setId(2L);
        RefreshToken existing = new RefreshToken();
        existing.setUser(u);
        existing.setToken("old");
        existing.setExpiresAt(Instant.now().plusSeconds(60));
        existing.setRevoked(false);

        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshToken rotated = service.rotate(existing);

        // existing should be revoked and saved
        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any());

        // new token should be for the same user and not revoked
        assertThat(rotated.getUser()).isEqualTo(u);
        assertThat(rotated.getToken()).isNotBlank();
        assertThat(rotated.isRevoked()).isFalse();
    }

    @Test
    void revoke_marksTokenAsRevoked() {
        RefreshToken t = new RefreshToken();
        t.setRevoked(false);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revoke(t);
        assertThat(t.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(t);
    }
}
