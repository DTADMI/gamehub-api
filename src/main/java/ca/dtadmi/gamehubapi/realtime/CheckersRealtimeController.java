package ca.dtadmi.gamehubapi.realtime;

import ca.dtadmi.gamehubapi.features.FeatureFlagsService;
import ca.dtadmi.gamehubapi.realtime.dto.RealtimeDtos.*;
import ca.dtadmi.gamehubapi.service.LeaderboardService;
import ca.dtadmi.gamehubapi.service.PresenceService;
import ca.dtadmi.gamehubapi.service.ProfanityFilter;
import ca.dtadmi.gamehubapi.service.RunIdService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Validated
@Controller
public class CheckersRealtimeController {

    private final SimpMessagingTemplate broker;
    private final ProfanityFilter profanityFilter;
    private final PresenceService presenceService;
    private final LeaderboardService leaderboardService;
    private final RunIdService runIdService;
    private final FeatureFlagsService flags;

    public CheckersRealtimeController(SimpMessagingTemplate broker,
                                      ProfanityFilter profanityFilter,
                                      PresenceService presenceService,
                                      LeaderboardService leaderboardService,
                                      RunIdService runIdService,
                                      FeatureFlagsService flags) {
        this.broker = broker;
        this.profanityFilter = profanityFilter;
        this.presenceService = presenceService;
        this.leaderboardService = leaderboardService;
        this.runIdService = runIdService;
        this.flags = flags;
    }

    private boolean enabled() {
        return flags.isEnabled("realtime_enabled") && flags.isEnabled("checkers_enabled");
    }

    @MessageMapping("/checkers/presence")
    public void presence(@Valid @Payload Envelope<@Valid PresenceIn> env, Principal principal) {
        if (!enabled() || env == null) return;
        String nickname = (env.user != null && env.user.nickname != null) ? env.user.nickname : "guest";
        String principalId = (principal != null && principal.getName() != null) ? principal.getName() : UUID.randomUUID().toString();
        String memberId = nickname + "|" + principalId;
        String roomId = (env.room != null && env.room.id != null) ? env.room.id : "checkers:global";

        String status = env.payload != null ? env.payload.status : "heartbeat";
        switch (status == null ? "heartbeat" : status) {
            case "join" -> presenceService.join(roomId, memberId);
            case "leave" -> presenceService.leave(roomId, memberId);
            default -> presenceService.heartbeat(roomId, memberId);
        }

        PresenceOut out = new PresenceOut();
        out.count = presenceService.count(roomId);
        out.users = presenceService.sample(roomId, 20).stream().map(k -> {
            PublicUser pu = new PublicUser();
            pu.id = k;
            pu.nickname = k.split("\\|")[0];
            return pu;
        }).collect(Collectors.toList());

        if (out.users == null) out.users = new ArrayList<>();
        boolean containsSelf = out.users.stream().anyMatch(u -> Objects.equals(u.nickname, nickname));
        if (!containsSelf) {
            PublicUser self = new PublicUser();
            self.id = memberId;
            self.nickname = nickname;
            out.users.add(self);
        }
        out.count = Math.max(out.count, out.users.size());

        Envelope<PresenceOut> res = new Envelope<>();
        res.type = "presence";
        res.room = env.room;
        res.user = env.user;
        res.payload = out;
        broker.convertAndSend("/topic/checkers/presence", res);
    }

    @MessageMapping("/checkers/leaderboard")
    public void leaderboard(@Valid @Payload Envelope<@Valid ScoreIn> env) {
        if (!enabled() || env == null || env.user == null || env.user.nickname == null || env.payload == null) return;
        int value = Math.max(0, env.payload.value);
        if (value > 1_000_000) return;
        if (flags.isEnabled("anti_cheat_enabled")) {
            String runId = env.payload.runId;
            if (!runIdService.validateAndConsume(runId)) return;
        }
        String scope = (env.room != null && env.room.id != null) ? env.room.id : "checkers:global";
        leaderboardService.submit(scope, env.user.nickname, value);

        LeaderboardOut out = new LeaderboardOut();
        List<Entry> serviceTop = leaderboardService.topN(scope, 10);
        out.top = new ArrayList<>(serviceTop == null ? Collections.emptyList() : serviceTop);
        boolean hasUser = out.top.stream().anyMatch(e -> Objects.equals(e.nickname, env.user.nickname));
        if (!hasUser) {
            Entry e = new Entry();
            e.nickname = env.user.nickname;
            e.value = value;
            out.top.add(0, e);
        }
        out.yourRank = leaderboardService.rankOf(scope, env.user.nickname);

        Envelope<LeaderboardOut> res = new Envelope<>();
        res.type = "leaderboard";
        res.room = env.room;
        res.user = env.user;
        res.payload = out;
        broker.convertAndSend("/topic/checkers/leaderboard", res);
    }

    @MessageMapping("/checkers/chat")
    public void chat(@Valid @Payload Envelope<@Valid ChatIn> env) {
        if (!enabled() || env == null || env.user == null || env.user.nickname == null || env.payload == null) return;
        String text = env.payload.text == null ? "" : env.payload.text.trim();
        if (text.isBlank()) return;
        text = profanityFilter.filter(text);
        if (text.length() > 300) text = text.substring(0, 300);

        ChatOut out = new ChatOut();
        out.nickname = env.user.nickname;
        out.text = text;

        Envelope<ChatOut> res = new Envelope<>();
        res.type = "chat";
        res.room = env.room;
        res.user = env.user;
        res.payload = out;
        broker.convertAndSend("/topic/checkers/chat", res);
    }

    @MessageMapping("/checkers/move")
    public void move(@Valid @Payload Envelope<@Valid MoveIn> env) {
        if (!enabled() || env == null || env.user == null || env.user.nickname == null || env.payload == null) return;
        // For M1: echo the move to subscribers without full rules validation.
        MoveOut out = new MoveOut();
        out.from = env.payload.from;
        out.to = env.payload.to;
        out.promo = env.payload.promo;
        out.notation = env.payload.notation;
        out.side = (env.user.role != null) ? env.user.role : "guest";

        Envelope<MoveOut> res = new Envelope<>();
        res.type = "move";
        res.room = env.room;
        res.user = env.user;
        res.payload = out;
        broker.convertAndSend("/topic/checkers/match", res);
    }
}
