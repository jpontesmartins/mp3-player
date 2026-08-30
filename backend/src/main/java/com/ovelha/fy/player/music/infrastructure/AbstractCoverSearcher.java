package com.ovelha.fy.player.music.infrastructure;

import com.ovelha.fy.player.music.domain.model.CoverImage;
import com.ovelha.fy.player.music.domain.port.AlbumCoverSearcher;
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

    /**
     * Construtor da classe base para buscadores de capa.
     *
     * @param downloader responsável pelo download das imagens
     * @param props propriedades de configuração (URL, timeouts, etc.)
     */
    protected AbstractCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        this.downloader = downloader;
        this.props = props;
    }

    /**
     * Busca a primeira imagem de capa para o termo de busca informado.
     *
     * @param query termo de busca (ex: "artista álbum")
     * @return imagem de capa encontrada, ou {@code null} se nenhuma for encontrada
     * @throws IOException se ocorrer erro de rede ou leitura
     */
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

    /**
     * Realiza a requisição HTTP GET e retorna o corpo da resposta.
     *
     * @param url URL da requisição
     * @return corpo da resposta HTTP
     * @throws IOException se ocorrer erro de rede ou timeout
     */
    private String fetchSearchResponse(String url) throws IOException {
        Connection.Response res = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .ignoreContentType(ignoreContentType())
                .execute();
        return res.body();
    }
}
