package com.mp3player.metadata.infrastructure;

import com.mp3player.metadata.infrastructure.CoverProperties;
import com.mp3player.metadata.domain.model.CoverImage;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Componente responsável por baixar imagens de capa de álbuns a partir de URLs.
 */
@Component
public class CoverDownloader {

    private static final Logger log = LoggerFactory.getLogger(CoverDownloader.class);

    private final CoverProperties props;

    /**
     * Construtor do download de capas.
     *
     * @param props propriedades de configuração (user agent, timeouts)
     */
    public CoverDownloader(CoverProperties props) {
        this.props = props;
    }

    /**
     * Baixa a imagem de capa a partir da URL informada.
     *
     * @param imageUrl URL da imagem a ser baixada
     * @return imagem de capa com bytes e content-type, ou {@code null} se o download retornar vazio
     * @throws IOException se ocorrer erro de rede ou timeout
     */
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
