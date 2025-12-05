package ca.dtadmi.gamehubapi.realtime;

import ca.dtadmi.gamehubapi.features.FeatureFlagsService;
import ca.dtadmi.gamehubapi.realtime.dto.RealtimeDtos.*;
import ca.dtadmi.gamehubapi.service.LeaderboardService;
import ca.dtadmi.gamehubapi.service.PresenceService;
import ca.dtadmi.gamehubapi.service.ProfanityFilter;
import ca.dtadmi.gamehubapi.service.RunIdService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckersRealtimeControllerTest {

    @Mock
    private SimpMessagingTemplate broker;
    @Mock
    private ProfanityFilter profanity;
    @Mock
    private PresenceService presenceService;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private RunIdService runIdService;
    @Mock
    private FeatureFlagsService flags;

    @InjectMocks
    private CheckersRealtimeController controller;

    private static Envelope<PresenceIn> presenceEnv(String nickname, String status) {
        Envelope<PresenceIn> env = new Envelope<>();
        env.type = "presence";
        env.room = new Room();
        env.room.id = "checkers:global";
        env.room.game = "checkers";
        env.user = new User();
        env.user.id = "guest";
        env.user.role = "guest";
        env.user.nickname = nickname;
        env.user.subscription = "free";
        env.payload = new PresenceIn();
        env.payload.status = status;
        return env;
    }

    @BeforeEach
    void setup() {
        lenient().when(profanity.filter(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(flags.isEnabled("realtime_enabled")).thenReturn(true);
        lenient().when(flags.isEnabled("checkers_enabled")).thenReturn(true);
        lenient().when(flags.isEnabled("anti_cheat_enabled")).thenReturn(false);
    }

    @Test
    void presence_join_broadcastsCount() {
        // Given
        when(presenceService.count(anyString())).thenReturn(1);
        when(presenceService.sample(anyString(), anyInt())).thenReturn(Collections.singletonList("Bo|test-id"));

        // When
        controller.presence(presenceEnv("Ari", "join"), null);

        // Then
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(broker).convertAndSend(eq("/topic/checkers/presence"), captor.capture());

        Envelope<?> sent = captor.getValue();
        assertThat(sent.type).isEqualTo("presence");
        PresenceOut out = (PresenceOut) sent.payload;
        assertThat(out.count).isGreaterThanOrEqualTo(1);
        assertThat(out.users).isNotNull();
        assertThat(out.users).hasSize(2);
        assertThat(out.users.get(0).nickname).isIn("Ari", "Bo");
    }

    @Test
    void score_valid_broadcastsLeaderboard() {
        // Given
        Entry testEntry = new Entry();
        testEntry.nickname = "Bo";
        testEntry.value = 123;
        when(leaderboardService.topN(anyString(), anyInt())).thenReturn(List.of(testEntry));
        when(leaderboardService.rankOf(anyString(), anyString())).thenReturn(1);

        Envelope<ScoreIn> env = new Envelope<>();
        env.type = "leaderboard";
        env.room = new Room();
        env.room.id = "checkers:global";
        env.user = new User();
        env.user.id = "u1";
        env.user.role = "user";
        env.user.nickname = "Bo";
        env.user.subscription = "free";
        env.payload = new ScoreIn();
        env.payload.value = 123;
        env.payload.runId = "dummy";

        // When
        controller.leaderboard(env);

        // Then
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(broker).convertAndSend(eq("/topic/checkers/leaderboard"), captor.capture());
        Envelope<?> sent = captor.getValue();
        assertThat(sent.type).isEqualTo("leaderboard");
        LeaderboardOut out = (LeaderboardOut) sent.payload;
        assertThat(out.top).extracting(e -> e.nickname).contains("Bo");
        assertThat(out.yourRank).isEqualTo(1);
    }
}
