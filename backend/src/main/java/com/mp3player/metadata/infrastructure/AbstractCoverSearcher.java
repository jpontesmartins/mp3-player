package com.mp3player.metadata.infrastructure;

import com.mp3player.metadata.infrastructure.CoverProperties;
import com.mp3player.metadata.domain.model.CoverImage;
import com.mp3player.metadata.domain.port.AlbumCoverSearcher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Classe base abstrata para buscadores de capa de álbum.
 * Implementa o Template Method com o fluxo: encode → HTTP GET → extrair URL → download.
 * Subclasses implementam os hooks {@link #buildSearchUrl} e {@link #extractImageUrl}.
 */
public abstract class AbstractCoverSearcher implements AlbumCoverSearcher {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final CoverDownloader downloader;
    protected final CoverProperties props;

    protected AbstractCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        this.downloader = downloader;
        this.props = props;
    }

    @Override
    public CoverImage findCover(String query) throws IOException {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = buildSearchUrl(encoded);
        log.info("[Capa] Buscando capa: {}", url);
        String responseBody = fetchSearchResponse(url);
        String imageUrl = extractImageUrl(responseBody);
        if (imageUrl == null) return null;
        return downloader.download(imageUrl);
    }

    /**
     * Monta a URL de busca completa a partir do termo codificado.
     *
     * @param encoded termo de busca URL-encoded
     * @return URL completa da API de busca
     */
    protected abstract String buildSearchUrl(String encoded);

    /**
     * Extrai a URL da imagem da resposta JSON da API.
     *
     * @param responseBody corpo da resposta HTTP
     * @return URL da imagem ou {@code null} se não encontrou
     */
    protected abstract String extractImageUrl(String responseBody);

    /**
     * Indica se o content-type deve ser ignorado na requisição de busca.
     * Padrão: {@code false}. Subclasses podem sobrescrever.
     *
     * @return {@code true} para ignorar content-type
     */
    protected boolean ignoreContentType() {
        return false;
    }

    private String fetchSearchResponse(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .ignoreContentType(ignoreContentType())
                .execute();
        return res.body();
    }
}
