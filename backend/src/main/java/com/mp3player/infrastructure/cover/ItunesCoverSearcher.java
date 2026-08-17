package com.mp3player.infrastructure.cover;

import com.mp3player.config.CoverProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItunesCoverSearcher extends AbstractCoverSearcher {

    private static final Pattern ARTWORK_URL = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"");

    public ItunesCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        super(downloader, props);
    }

    @Override
    protected String buildSearchUrl(String encoded) {
        return props.itunesUrl() + encoded;
    }

    @Override
    protected String extractImageUrl(String responseBody) {
        Matcher matcher = ARTWORK_URL.matcher(responseBody);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("100x100bb", "600x600bb");
    }
}
