package com.mp3player.metadata.infrastructure;

import com.mp3player.metadata.infrastructure.CoverProperties;
import com.mp3player.metadata.domain.model.CoverImage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CoverDownloader {

    private static final Logger log = LoggerFactory.getLogger(CoverDownloader.class);

    private final CoverProperties props;

    public CoverDownloader(CoverProperties props) {
        this.props = props;
    }

    public CoverImage download(String imageUrl) throws IOException {
        log.info("[Capa] Baixando capa de {}", imageUrl);
        Connection.Response res = Jsoup.connect(imageUrl)
                .userAgent(props.userAgent())
                .ignoreContentType(true)
                .timeout(props.timeoutDownload())
                .execute();
        byte[] bytes = res.bodyAsBytes();
        if (bytes.length == 0) return null;
        String contentType = res.contentType();
        if (contentType == null) contentType = "image/jpeg";
        return new CoverImage(bytes, contentType);
    }
}
