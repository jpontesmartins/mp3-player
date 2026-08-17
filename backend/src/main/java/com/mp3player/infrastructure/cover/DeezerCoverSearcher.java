package com.mp3player.infrastructure.cover;

import com.mp3player.config.CoverProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeezerCoverSearcher extends AbstractCoverSearcher {

    private static final Pattern DEEZER_COVER = Pattern.compile("\"cover_xl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEEZER_MEDIUM = Pattern.compile("\"cover_medium\"\\s*:\\s*\"([^\"]+)\"");

    public DeezerCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        super(downloader, props);
    }

    @Override
    protected String buildSearchUrl(String encoded) {
        return props.deezerUrl() + encoded;
    }

    @Override
    protected String extractImageUrl(String responseBody) {
        Matcher big = DEEZER_COVER.matcher(responseBody);
        if (big.find()) return big.group(1);
        Matcher medium = DEEZER_MEDIUM.matcher(responseBody);
        return medium.find() ? medium.group(1) : null;
    }

    @Override
    protected boolean ignoreContentType() {
        return true;
    }
}
