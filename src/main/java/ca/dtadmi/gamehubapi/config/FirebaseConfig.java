package ca.dtadmi.gamehubapi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Configuration
@ConfigurationProperties(prefix = "firebase")
@EnableConfigurationProperties
@Data
public class FirebaseConfig {
    private static final Logger LOGGER = Logger.getLogger(FirebaseConfig.class.getName());
    private String type;
    private String projectId;
    private String privateKeyId;
    private String privateKey;
    private String clientEmail;
    private String clientId;
    private String authUri;
    private String tokenUri;
    private String authProviderX509CertUrl;
    private String clientX509CertUrl;
    private String universeDomain;
    /*@Value("${firebase.type}")
    private String type;*/
    @Value("${DARK_SECRET:}")
    private String darkSecret;

    @Profile("prod")
    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${firebase.privateKey:}')")
    public FirebaseApp firebaseApp() throws IOException {
        LOGGER.log(Level.FINE, "Dark secret: {0}", darkSecret);
        LOGGER.log(Level.FINE, "Private key: {0}", privateKey);
        LOGGER.log(Level.FINE, "Private key ID: {0}", privateKeyId);
        LOGGER.log(Level.FINE, "Client email: {0}", clientEmail);
        LOGGER.log(Level.FINE, "Client ID: {0}", clientId);
        LOGGER.log(Level.FINE, "Auth URI: {0}", authUri);
        LOGGER.log(Level.FINE, "Token URI: {0}", tokenUri);
        LOGGER.log(Level.FINE, "Auth provider X509 cert URL: {0}", authProviderX509CertUrl);
        LOGGER.log(Level.FINE, "Client X509 cert URL: {0}", clientX509CertUrl);
        LOGGER.log(Level.FINE, "Universe domain: {0}", universeDomain);
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new IllegalStateException("Firebase private key is not set. Please check your configuration.");
        }

        // Decode the base64-encoded private key
        //byte[] decodedKey = Base64.decodeBase64(privateKey);
        Map<String, Object> serviceAccount = getServiceAccountMap(/*decodedKey*/);

        // Convert to JSON string
        String serviceAccountJson = new com.google.gson.Gson().toJson(serviceAccount);

        LOGGER.log(Level.FINE, "Service account JSON: {0}", serviceAccountJson);

        try (InputStream serviceAccountStream = new ByteArrayInputStream(serviceAccountJson.getBytes())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                    .setProjectId(projectId)
                    .build();
            LOGGER.log(Level.INFO, "Initializing Firebase app with project ID: {0}", projectId);
            return FirebaseApp.initializeApp(options);
        }
    }

    private @NotNull Map<String, Object> getServiceAccountMap(/*byte[] decodedKey*/) {
        // The private key might be base64 encoded or already in the correct format
        String privateKeyContent = privateKey;
        if (!privateKey.trim().startsWith("-----")) {
            // Try to decode as base64 if it doesn't look like a PEM key
            try {
                privateKeyContent = new String(Base64.decodeBase64(privateKey)).replace("\\n", "\n");
            } catch (Exception e) {
                // If decoding fails, use the key as-is
                privateKeyContent = privateKey;
            }
        }
        //String decodedPrivateKey  = new String(decodedKey).replace("\\n", "\n");

        // Create the service account JSON structure
        Map<String, Object> serviceAccount = new HashMap<>();
        serviceAccount.put("type", type);
        serviceAccount.put("project_id", projectId);
        serviceAccount.put("private_key_id", privateKeyId);
        serviceAccount.put("private_key", privateKeyContent);
        serviceAccount.put("client_email", clientEmail);
        serviceAccount.put("client_id", clientId);
        serviceAccount.put("auth_uri", authUri);
        serviceAccount.put("token_uri", tokenUri);
        serviceAccount.put("auth_provider_x509_cert_url", authProviderX509CertUrl);
        serviceAccount.put("client_x509_cert_url", clientX509CertUrl);
        serviceAccount.put("universe_domain", universeDomain);
        return serviceAccount;
    }

    @Bean
    @ConditionalOnBean(FirebaseApp.class)
    public FirebaseAuth firebaseAuth() throws IOException {
        return FirebaseAuth.getInstance(firebaseApp());
    }
}
