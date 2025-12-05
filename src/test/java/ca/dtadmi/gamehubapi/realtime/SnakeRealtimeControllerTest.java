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

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
//@Disabled("Temporarily disabled during backend API alignment")
class SnakeRealtimeControllerTest {

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
    private SnakeRealtimeController controller;

    private static Envelope<PresenceIn> presenceEnv(String nickname, String status) {
        Envelope<PresenceIn> env = new Envelope<>();
        env.type = "presence";
        env.room = new Room();
        env.room.id = "snake:global";
        env.room.game = "snake";
        env.user = new User();
        env.user.nickname = nickname;
        env.user.role = "guest";
        env.user.subscription = "free";
        PresenceIn in = new PresenceIn();
        in.status = status;
        env.payload = in;
        return env;
    }

    private static Envelope<ScoreIn> scoreEnv(String nickname, int value) {
        Envelope<ScoreIn> env = new Envelope<>();
        env.type = "score";
        env.room = new Room();
        env.room.id = "snake:global";
        env.room.game = "snake";
        env.user = new User();
        env.user.nickname = nickname;
        env.user.role = "guest";
        env.user.subscription = "free";
        ScoreIn in = new ScoreIn();
        in.value = value;
        env.payload = in;
        return env;
    }

    private static Envelope<ChatIn> chatEnv(String nickname, String text) {
        Envelope<ChatIn> env = new Envelope<>();
        env.type = "chat";
        env.room = new Room();
        env.room.id = "snake:global";
        env.room.game = "snake";
        env.user = new User();
        env.user.nickname = nickname;
        env.user.role = "guest";
        env.user.subscription = "free";
        ChatIn in = new ChatIn();
        in.text = text;
        env.payload = in;
        return env;
    }

    @BeforeEach
    void setup() {
        /*broker = mock(SimpMessagingTemplate.class);
        profanity = mock(ProfanityFilter.class);
        presenceService = mock(PresenceService.class);
        leaderboardService = mock(LeaderboardService.class);
        runIdService = mock(RunIdService.class);
        flags = mock(FeatureFlagsService.class);*/

        // Default mock behaviors
        lenient().when(profanity.filter(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(flags.isEnabled("realtime_enabled")).thenReturn(true);
        lenient().when(flags.isEnabled("anti_cheat_enabled")).thenReturn(false);

        /*// Mock leaderboard service to return a test entry
        Entry testEntry = new Entry();
        testEntry.nickname = "Bo";
        testEntry.value = 123;
        when(leaderboardService.topN(anyString(), anyInt())).thenReturn(List.of(testEntry));

        // Mock presence service
        when(presenceService.count(anyString())).thenReturn(1);
        when(presenceService.sample(anyString(), anyInt())).thenReturn(Collections.singletonList("Bo|test-id"));*/

        //controller = new SnakeRealtimeController(broker, profanity, presenceService, leaderboardService, runIdService, flags);
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
        verify(broker).convertAndSend(eq("/topic/snake/presence"), captor.capture());

        Envelope<?> sent = captor.getValue();
        assertThat(sent.type).isEqualTo("presence");
        PresenceOut out = (PresenceOut) sent.payload;
        assertThat(out.count).isGreaterThanOrEqualTo(1);
        assertThat(out.users).isNotNull();
        assertThat(out.users).hasSize(2);
        assertThat(out.users.get(0).nickname).isIn("Ari", "Bo"); // Either the test user or the mocked one
    }

    private void setRealtimeEnabled(boolean v) {
        when(flags.isEnabled("realtime_enabled")).thenReturn(v);
    }

    @Test
    void score_oversized_isIgnored() {
        controller.score(scoreEnv("Bo", 1_000_001));
        verify(broker, never()).convertAndSend(eq("/topic/snake/leaderboard"), (Object) any());
    }

    @Test
    void score_valid_broadcastsLeaderboard() {
        // Given
        Entry testEntry = new Entry();
        testEntry.nickname = "Bo";
        testEntry.value = 123;
        when(leaderboardService.topN(anyString(), anyInt())).thenReturn(List.of(testEntry));

        // When
        controller.score(scoreEnv("Bo", 123));

        // Then
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(broker).convertAndSend(eq("/topic/snake/leaderboard"), captor.capture());

        Envelope<?> sent = captor.getValue();
        assertThat(sent.type).isEqualTo("leaderboard");
        LeaderboardOut out = (LeaderboardOut) sent.payload;
        assertThat(out.top).isNotEmpty();
        assertThat(out.top.get(0).nickname).isEqualTo("Bo");
        assertThat(out.top).extracting(e -> e.nickname).contains("Bo");
    }

    @Test
    void chat_blank_isIgnored() {
        controller.chat(chatEnv("Chen", "   "));
        verify(broker, never()).convertAndSend(eq("/topic/snake/chat"), (Object) any());
    }

    @Test
    void chat_filtersProfanity_andBroadcasts() {
        when(profanity.filter("badword here")).thenReturn("******* here");
        controller.chat(chatEnv("Chen", "badword here"));
        ArgumentCaptor<Envelope> captor = ArgumentCaptor.forClass(Envelope.class);
        verify(broker).convertAndSend(eq("/topic/snake/chat"), captor.capture());
        Envelope<?> sent = captor.getValue();
        ChatOut out = (ChatOut) sent.payload;
        assertThat(out.text).contains("*");
    }

    @Test
    void featureFlag_off_preventsBroadcasts() {
        setRealtimeEnabled(false);
        controller.presence(presenceEnv("Ari", "join"), null);
        controller.score(scoreEnv("Bo", 123));
        controller.chat(chatEnv("Chen", "hi"));
        verifyNoInteractions(broker);
    }

    private record TestPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
