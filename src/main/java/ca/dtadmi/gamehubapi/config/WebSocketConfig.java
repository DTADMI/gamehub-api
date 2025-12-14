package ca.dtadmi.gamehubapi.config;

import ca.dtadmi.gamehubapi.security.CustomUserDetailsService;
import ca.dtadmi.gamehubapi.security.JwtTokenProvider;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "features.realtime_enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final org.springframework.beans.factory.ObjectProvider<org.springframework.messaging.support.ChannelInterceptor> stompRateLimiterProvider;

    public WebSocketConfig(JwtTokenProvider tokenProvider, CustomUserDetailsService userDetailsService,
                           @org.springframework.beans.factory.annotation.Qualifier("stompRateLimiterInterceptor")
                           org.springframework.beans.factory.ObjectProvider<org.springframework.messaging.support.ChannelInterceptor> stompRateLimiterProvider) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
        this.stompRateLimiterProvider = stompRateLimiterProvider;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setInterceptors(httpSessionHandshakeInterceptor());
    }

    @Bean
    public HandshakeInterceptor httpSessionHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                           WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (request instanceof ServletServerHttpRequest servletRequest) {
                    HttpSession session = servletRequest.getServletRequest().getSession();
                    attributes.put("sessionId", session.getId());
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {
            }
        };
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        var rl = stompRateLimiterProvider.getIfAvailable();
        if (rl != null) {
            registration.interceptors(channelInterceptor(), rl);
        } else {
            registration.interceptors(channelInterceptor());
        }
    }

    @Bean
    public ChannelInterceptor channelInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Try to set Principal from JWT "Authorization" connect header if provided
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            if (tokenProvider.validateToken(token)) {
                                String username = tokenProvider.getUsernameFromJWT(token);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                Authentication authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                                accessor.setUser(authentication);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                    // Fallback to request security context (may be null)
                    if (accessor.getUser() == null) {
                        Authentication user = SecurityContextHolder.getContext().getAuthentication();
                        accessor.setUser(user);
                    }
                }
                return message;
            }
        };
    }

    // STOMP rate limiter bean moved to StompInterceptorsConfig to avoid circular reference
}
