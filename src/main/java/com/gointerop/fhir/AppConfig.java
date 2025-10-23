package com.gointerop.fhir;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.gointerop.fhir.hapi.HapiRestfulServer;

import ca.uhn.fhir.rest.server.RestfulServer;

@Configuration
@ComponentScan(basePackages = "com.gointerop.fhir")
public class AppConfig {
    @Autowired
    Environment env;

    @Autowired
    private HapiRestfulServer jpaRestfulServer;

    @Bean
    public ServletRegistrationBean<RestfulServer> servletRegistrationBean() {
        ServletRegistrationBean<RestfulServer> servletRegistrationBean = new ServletRegistrationBean<>();
        servletRegistrationBean.setServlet(jpaRestfulServer);
        servletRegistrationBean.addUrlMappings("/r4/*");
        servletRegistrationBean.setLoadOnStartup(1);

        return servletRegistrationBean;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        FilterRegistrationBean<CorsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(corsFilter());
        registrationBean.addUrlPatterns("/*"); // Apply to all URLs
        return registrationBean;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*")); // Allow any origin
        config.addAllowedHeader("*"); // Allow any header
        config.addAllowedMethod("*"); // Allow any method
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
