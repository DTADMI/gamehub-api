package ca.dtadmi.gamehubapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.DefaultResponseErrorHandler;

import java.io.IOException;

@TestConfiguration(proxyBeanMethods = false)
public class TestRestTemplateNoErrorConfig {

    @Bean
    RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder()
                .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
                .errorHandler(new DefaultResponseErrorHandler() {
                    @Override
                    public boolean hasError(ClientHttpResponse response) throws IOException {
                        // Treat all statuses as non-errors in tests; assertions will check status codes explicitly
                        return false;
                    }
                });
    }
}
