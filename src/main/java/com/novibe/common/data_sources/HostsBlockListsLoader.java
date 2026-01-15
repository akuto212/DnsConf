package com.novibe.common.data_sources;

import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.novibe.common.config.EnvironmentVariables.TARGET_IP;

@Service
@Setter(onMethod_ = @Autowired)
public class HostsBlockListsLoader extends ListLoader<String> {

    private static final String[] BLOCK_PREFIXES = { "0.0.0.0 ", "127.0.0.1 ", "::1 "};
    private static final Set<String> LOCALHOST_NAME = Set.of("localhost", "ip6-localhost");
    private static final String HOSTS_GENERATOR_URL = "https://raw.githubusercontent.com/ImMALWARE/dns.malw.link/refs/heads/master/hosts";

    private HostsGenerator hostsGenerator;

    public static boolean isBlock(String line) {
        for (String blockPrefix : BLOCK_PREFIXES) {
            if (line.startsWith(blockPrefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Stream<String> lineParser(String urlList) {
        return Pattern.compile("\\r?\\n").splitAsStream(urlList)
                .parallel()
                .map(String::strip)
                .filter(str -> !str.isBlank())
                .filter(line -> !line.startsWith("#"))
                .filter(HostsBlockListsLoader::isBlock)
                .map(this::removeIp)
                .map(String::toLowerCase)
                .filter(domain -> !LOCALHOST_NAME.contains(domain));
    }

    private String removeIp(String line) {
        for (String blockPrefix : BLOCK_PREFIXES) {
            if (line.startsWith(blockPrefix)) {
                return line.substring(blockPrefix.length());
            }
        }
        return line;
    }

    @Override
    protected String listType() {
        return "Block";
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("preview")
    public List<String> fetchWebsites(List<String> urls) {
        // Check if we should use the hosts generator
        boolean useGenerator = TARGET_IP != null && !TARGET_IP.isBlank()
                && urls.contains(HOSTS_GENERATOR_URL);

        if (useGenerator) {
            // Generate hosts file and parse it
            String generatedHosts = hostsGenerator.generateHosts();
            List<String> fromGenerator = lineParser(generatedHosts)
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
            List<String> fromOtherUrls = super.fetchWebsites(otherUrls);

            // Combine results
            List<String> combined = new ArrayList<>(fromGenerator);
            combined.addAll(fromOtherUrls);

            return combined.stream().distinct().collect(Collectors.toCollection(ArrayList::new));
        }

        // If not using generator, use default parent implementation
        return super.fetchWebsites(urls);
    }

}
