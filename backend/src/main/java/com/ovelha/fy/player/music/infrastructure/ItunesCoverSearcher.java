package com.ovelha.fy.player.music.infrastructure;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Buscador de capas que utiliza a API de busca do iTunes.
 */
public class ItunesCoverSearcher extends AbstractCoverSearcher {

    private static final Pattern ARTWORK_URL = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Construtor do buscador do iTunes.
     *
     * @param downloader responsável pelo download das imagens
     * @param props propriedades de configuração (URL, timeouts, etc.)
     */
    public ItunesCoverSearcher(CoverDownloader downloader, CoverProperties props) {
        super(downloader, props);
    }

    @Override
    protected String buildSearchUrl(String encoded) {
        return props.itunesUrl() + encoded;
    }

    /**
     * Extrai a URL da imagem da resposta JSON da API do iTunes.
     *
     * @param responseBody corpo da resposta HTTP
     * @return URL da imagem (600x600) ou {@code null} se não encontrou
     */
    @Override
    protected String extractImageUrl(String responseBody) {
        Matcher matcher = ARTWORK_URL.matcher(responseBody);
        if (!matcher.find()) return null;
        return matcher.group(1).replace("100x100bb", "600x600bb");
    }
}
