package com.novibe.common.data_sources;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class HostsBlockListsLoader extends ListLoader<String> {

    private static final String[] BLOCK_PREFIXES = { "0.0.0.0 ", "127.0.0.1 ", "::1 "};
    private static final Set<String> LOCALHOST_NAME = Set.of("localhost", "ip6-localhost");

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


}
