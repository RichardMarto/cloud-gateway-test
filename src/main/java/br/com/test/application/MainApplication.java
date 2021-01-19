package br.com.test.application;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({
        "br.com.test*",
})

@SpringBootApplication
public class MainApplication {
    private static final String FORWARDED_URL = "X-CF-Forwarded-Url";

    private static final String PROXY_METADATA = "X-CF-Proxy-Metadata";

    private static final String PROXY_SIGNATURE = "X-CF-Proxy-Signature";

    public static void main(String[] args) {
        new SpringApplicationBuilder(MainApplication.class)
                .web(WebApplicationType.REACTIVE)
                .run(args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder
                .routes()
                .route(route -> route.header(FORWARDED_URL, ".*")
                        .and()
                        .header(PROXY_METADATA, ".*")
                        .and()
                        .header(PROXY_SIGNATURE, ".*")
                        .uri("http://google.com:80"))
                .build();
    }
}