package com.mp3player.infrastructure.cover;

import com.mp3player.config.CoverProperties;
import com.mp3player.domain.model.CoverImage;
import com.mp3player.domain.port.AlbumCoverSearcher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeezerCoverSearcher implements AlbumCoverSearcher {

    private static final Logger log = LoggerFactory.getLogger(DeezerCoverSearcher.class);
    private static final Pattern DEEZER_COVER = Pattern.compile("\"cover_xl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEEZER_MEDIUM = Pattern.compile("\"cover_medium\"\\s*:\\s*\"([^\"]+)\"");

    private final CoverDownloader downloader;
    private final CoverProperties props;

    public DeezerCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        this.downloader = downloader;
        this.props = props;
    }

    @Override
    public CoverImage findCover(String query) throws IOException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = props.deezerUrl() + encoded;
        log.info("[Capa] Buscando capa no Deezer: {}", url);
        String json = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .ignoreContentType(true)
                .execute()
                .body();
        Matcher big = DEEZER_COVER.matcher(json);
        if (big.find()) return downloader.download(big.group(1));
        Matcher medium = DEEZER_MEDIUM.matcher(json);
        return medium.find() ? downloader.download(medium.group(1)) : null;
    }
}
