package ca.dtadmi.gamehubapi.config;

import ca.dtadmi.gamehubapi.logging.RequestIdFilter;
import jakarta.servlet.Filter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public FilterRegistrationBean<Filter> requestIdFilterRegistration() {
        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestIdFilter());
        bean.setOrder(1); // early in chain
        bean.addUrlPatterns("/*");
        return bean;
    }
}
