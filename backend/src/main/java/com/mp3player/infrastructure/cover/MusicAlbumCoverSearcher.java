package com.mp3player.infrastructure.cover;

import com.mp3player.config.CoverProperties;
import com.mp3player.domain.model.CoverImage;
import com.mp3player.domain.port.AlbumCoverSearcher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Buscador de capas de álbum que consulta a API de busca da Apple Music/iTunes e,
 * como fallback, a API do Deezer. Ambas devolvem a arte do álbum em alta
 * resolução sem exigir chave ou JavaScript. O scrap de imagens do Google/Bing de
 * tornou inviável (Google exige JS; Bing retorna resultados irrelevantes).
 */
@Component
public class MusicAlbumCoverSearcher implements AlbumCoverSearcher {

    private static final Logger log = LoggerFactory.getLogger(MusicAlbumCoverSearcher.class);
    private static final Pattern ARTWORK_URL = Pattern.compile("\"artworkUrl100\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEEZER_COVER = Pattern.compile("\"cover_xl\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DEEZER_MEDIUM = Pattern.compile("\"cover_medium\"\\s*:\\s*\"([^\"]+)\"");

    private final CoverProperties props;

    public MusicAlbumCoverSearcher(CoverProperties props) {
        this.props = props;
    }

    @Override
    public CoverImage findCover(String term) throws IOException {
        String encoded = URLEncoder.encode(term, StandardCharsets.UTF_8);

        String imageUrl = findItunes(encoded);
        if (imageUrl == null) {
            log.info("[Capa] iTunes não encontrou capa para \"{}\", tentando Deezer", term);
            imageUrl = findDeezer(encoded);
        }

        if (imageUrl == null) {
            log.warn("[Capa] Nenhuma capa encontrada para: {}", term);
            return null;
        }

        log.info("[Capa] Baixando capa de {}", imageUrl);
        return download(imageUrl);
    }

    /** Consulta a API de busca do iTunes (entity=album) e retorna a capa do primeiro álbum. */
    private String findItunes(String encoded) throws IOException {
        String url = props.itunesUrl() + encoded;
        log.info("[Capa] Buscando capa no iTunes: {}", url);
        Connection.Response res = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .execute();
        Matcher matcher = ARTWORK_URL.matcher(res.body());
        if (!matcher.find()) return null;
        return matcher.group(1).replace("100x100bb", "600x600bb");
    }

    /** Consulta a API da Deezer e retorna a capa grande do primeiro álbum. */
    private String findDeezer(String encoded) throws IOException {
        String url = props.deezerUrl() + encoded;
        log.info("[Capa] Buscando capa no Deezer: {}", url);
        String json = Jsoup.connect(url)
                .userAgent(props.userAgent())
                .timeout(props.timeoutConnect())
                .ignoreContentType(true)
                .execute()
                .body();
        Matcher big = DEEZER_COVER.matcher(json);
        if (big.find()) return big.group(1);
        Matcher medium = DEEZER_MEDIUM.matcher(json);
        return medium.find() ? medium.group(1) : null;
    }

    /** Baixa os bytes da capa, ignorando o tipo de conteúdo. */
    private CoverImage download(String imageUrl) throws IOException {
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