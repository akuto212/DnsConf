package com.novibe.common.config;

import com.google.gson.Gson;
import com.novibe.common.data_sources.HostsGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.http.HttpClient;
import java.util.UUID;

import static com.novibe.common.config.EnvironmentVariables.TARGET_IP;

@Configuration
public class AppConfig {

    @Bean
    Gson gson() {
        return new Gson();
    }

    @Bean
    String sessionId() {
        return UUID.randomUUID().toString();
    }

    @Bean
    HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    HostsGenerator hostsGenerator(HttpClient httpClient) {
        String targetIp = TARGET_IP != null && !TARGET_IP.isBlank() ? TARGET_IP : "0.0.0.0";
        return new HostsGenerator(httpClient, targetIp);
    }

}
