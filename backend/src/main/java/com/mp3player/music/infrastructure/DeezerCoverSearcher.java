package com.mp3player.music.infrastructure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Buscador de capas que utiliza a API de busca do Deezer.
 */
public class DeezerCoverSearcher extends AbstractCoverSearcher {

    private static final Pattern DEEZER_COVER = Pattern.compile("\"cover_xl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEEZER_MEDIUM = Pattern.compile("\"cover_medium\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Construtor do buscador do Deezer.
     *
     * @param downloader responsável pelo download das imagens
     * @param props propriedades de configuração (URL, timeouts, etc.)
     */
    public DeezerCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        super(downloader, props);
    }

    @Override
    protected String buildSearchUrl(String encoded) {
        return props.deezerUrl() + encoded;
    }

    /**
     * Extrai a URL da imagem da resposta JSON da API do Deezer.
     * Prioriza a imagem XL; se não encontrar, usa a média.
     *
     * @param responseBody corpo da resposta HTTP
     * @return URL da imagem ou {@code null} se não encontrou
     */
    @Override
    protected String extractImageUrl(String responseBody) {
        Matcher big = DEEZER_COVER.matcher(responseBody);
        if (big.find()) return unescape(big.group(1));
        Matcher medium = DEEZER_MEDIUM.matcher(responseBody);
        return medium.find() ? unescape(medium.group(1)) : null;
    }

    /**
     * Remove escapes de barras presentes na resposta JSON do Deezer.
     *
     * @param s string com escapes
     * @return string sem escapes de barra
     */
    private static String unescape(String s) {
        return s.replace("\\/", "/");
    }

    /**
     * Indica que o content-type deve ser ignorado na requisição de busca.
     *
     * @return {@code true} para ignorar content-type
     */
    @Override
    protected boolean ignoreContentType() {
        return true;
    }
}
