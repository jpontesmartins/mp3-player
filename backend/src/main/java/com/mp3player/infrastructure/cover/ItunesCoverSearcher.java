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

public class ItunesCoverSearcher implements AlbumCoverSearcher {

    private static final Logger log = LoggerFactory.getLogger(ItunesCoverSearcher.class);
    private static final Pattern ARTWORK_URL = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"");

    private final CoverDownloader downloader;
    private final CoverProperties props;

    public ItunesCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        this.downloader = downloader;
        this.props = props;
    }

    @Override
    public CoverImage findCover(String query) throws IOException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = props.itunesUrl() + encoded;
        log.info("[Capa] Buscando capa no iTunes: {}", url);
        Connection.Response res = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .execute();
        Matcher matcher = ARTWORK_URL.matcher(res.body());
        if (!matcher.find()) return null;
        String imageUrl = matcher.group(1).replace("100x100bb", "600x600bb");
        return downloader.download(imageUrl);
    }
}
