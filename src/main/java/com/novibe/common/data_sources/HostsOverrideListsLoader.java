package com.novibe.common.data_sources;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.novibe.common.config.EnvironmentVariables.TARGET_IP;

@Service
@Setter(onMethod_ = @Autowired)
public class HostsOverrideListsLoader extends ListLoader<HostsOverrideListsLoader.BypassRoute> {

    private static final String HOSTS_GENERATOR_URL = "https://raw.githubusercontent.com/ImMALWARE/dns.malw.link/refs/heads/master/hosts";

    private HostsGenerator hostsGenerator;

    public record BypassRoute(String ip, String website) {
    }

    @Override
    protected Stream<BypassRoute> lineParser(String urlList) {
        return Pattern.compile("\\r?\\n").splitAsStream(urlList)
                .parallel()
                .map(String::strip)
                .filter(str -> !str.isBlank())
                .filter(line -> !line.startsWith("#"))
                .filter(line -> !HostsBlockListsLoader.isBlock(line))
                .map(this::mapLine);
    }

    @Override
    protected String listType() {
        return "Override";
    }

    private BypassRoute mapLine(String line) {
        int delimiter = line.indexOf(" ");
        String ip = line.substring(0, delimiter++);
        String website = line.substring(delimiter);
        return new BypassRoute(ip, website);
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("preview")
    public List<BypassRoute> fetchWebsites(List<String> urls) {
        // Check if we should use the hosts generator
        boolean useGenerator = TARGET_IP != null && !TARGET_IP.isBlank()
                && urls.contains(HOSTS_GENERATOR_URL);

        if (useGenerator) {
            // Generate hosts file and parse it
            String generatedHosts = hostsGenerator.generateHosts();
            List<BypassRoute> fromGenerator = lineParser(generatedHosts)
                    .distinct()
                    .collect(Collectors.toCollection(ArrayList::new));

            // Fetch and parse other URLs (excluding the generator URL)
            List<String> otherUrls = urls.stream()
                    .filter(url -> !HOSTS_GENERATOR_URL.equals(url))
                    .toList();

            if (otherUrls.isEmpty()) {
                return fromGenerator;
            }

            // Fetch other URLs using parent implementation
            List<BypassRoute> fromOtherUrls = super.fetchWebsites(otherUrls);

            // Combine results
            List<BypassRoute> combined = new ArrayList<>(fromGenerator);
            combined.addAll(fromOtherUrls);

            return combined.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        }

        // If not using generator, use default parent implementation
        return super.fetchWebsites(urls);
    }

}
