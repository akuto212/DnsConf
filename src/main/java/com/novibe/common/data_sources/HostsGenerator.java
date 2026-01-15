package com.novibe.common.data_sources;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Generates a custom hosts file by fetching from a source URL and replacing IP addresses
 */
@Slf4j
@RequiredArgsConstructor
public class HostsGenerator {
    private static final String HOSTS_URL = "https://raw.githubusercontent.com/ImMALWARE/dns.malw.link/refs/heads/master/hosts";
    private static final Pattern IP_REGEX = Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\s+.+$");

    private final HttpClient client;
    private final String targetIp;

    /**
     * Fetches the hosts file from the source URL and processes it
     * @return processed hosts file content with replaced IPs
     */
    public String generateHosts() {
        try {
            log.info("Fetching hosts file from: {}", HOSTS_URL);
            String content = fetchHostsFile();
            log.info("Processing hosts file with target IP: {}", targetIp);
            return processHosts(content);
        } catch (Exception e) {
            log.error("Failed to generate hosts file", e);
            throw new RuntimeException("Failed to generate hosts file", e);
        }
    }

    private String fetchHostsFile() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(HOSTS_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }

            return response.body();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch hosts file from " + HOSTS_URL, e);
        }
    }

    private String processHosts(String content) {
        String[] lines = content.split("\\r?\\n");

        // Skip first 2 lines as per JS code logic (header and date)
        StringBuilder output = new StringBuilder();

        // Add custom header
        output.append("### Akuto hosts file\n");
        output.append("# Домены взяты из этих источников:\n");
        output.append("# https://info.dns.malw.link/hosts\n");
        output.append("\n");

        // Process remaining lines
        for (int i = 2; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.stripTrailing();

            // Keep empty lines and comments as is
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                output.append(trimmed).append("\n");
                continue;
            }

            // Skip lines starting with 0.0.0.0
            if (trimmed.startsWith("0.0.0.0")) {
                continue;
            }

            // Process lines matching IP pattern
            if (IP_REGEX.matcher(trimmed).matches()) {
                String[] parts = trimmed.split("\\s+", 2);
                if (parts.length == 2) {
                    output.append(targetIp).append(" ").append(parts[1]).append("\n");
                } else {
                    output.append(trimmed).append("\n");
                }
            } else {
                output.append(trimmed).append("\n");
            }
        }

        return output.toString();
    }
}
