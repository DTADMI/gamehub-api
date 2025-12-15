package ca.dtadmi.gamehubapi.websocket;

import ca.dtadmi.gamehubapi.service.GameService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "features.realtime_enabled", havingValue = "true", matchIfMissing = true)
public class GameSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(GameSessionManager.class);

    private final Map<String, GameSession> activeSessions = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    public GameSessionManager(SimpMessagingTemplate messagingTemplate, GameService gameService) {
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
    }

    @Scheduled(fixedRate = 5000)
    public void cleanupInactiveSessions() {
        // Clean up inactive sessions (placeholder)
    }

    public GameSession createSession(String gameType, String hostUserId) {
        String sessionId = UUID.randomUUID().toString();
        GameSession session = new GameSession(sessionId, gameType, hostUserId);
        activeSessions.put(sessionId, session);
        return session;
    }

    public void joinSession(String sessionId, String userId) {
        GameSession session = activeSessions.get(sessionId);
        if (session != null && !session.isFull()) {
            session.addPlayer(userId);
            broadcastSessionUpdate(session);
        }
    }

    private void broadcastSessionUpdate(GameSession session) {
        messagingTemplate.convertAndSend("/topic/session/" + session.getId(), session);
    }

    public static class GameSession {
        private final String id;
        private final String gameType;
        private final String hostUserId;
        private final List<String> players = new ArrayList<>();
        private final int maxPlayers = 2;

        public GameSession(String id, String gameType, String hostUserId) {
            this.id = id;
            this.gameType = gameType;
            this.hostUserId = hostUserId;
            this.players.add(hostUserId);
        }

        public String getId() {
            return id;
        }

        public String getGameType() {
            return gameType;
        }

        public String getHostUserId() {
            return hostUserId;
        }

        public List<String> getPlayers() {
            return players;
        }

        public boolean isFull() {
            return players.size() >= maxPlayers;
        }

        public void addPlayer(String userId) {
            if (!isFull()) players.add(userId);
        }
    }
}
