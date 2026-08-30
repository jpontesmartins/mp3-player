package com.mp3player.music.application;

import com.mp3player.music.domain.model.CoverImage;
import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.music.domain.port.AlbumCoverSearcher;
import com.mp3player.music.domain.port.Id3Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mp3player.shared.domain.util.MusicFileNaming;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service da aplicação para download de capas de álbum: lê os metadados ID3 do
 * arquivo, monta a busca por "artista + álbum + cover" e salva a primeira imagem
 * encontrada na pasta do álbum.
 */
@Service
public class CoverService {

    private static final Logger log = LoggerFactory.getLogger(CoverService.class);

    private final Id3Codec id3Codec;
    private final AlbumCoverSearcher coverSearcher;

    /**
     * Construtor do serviço de download de capas.
     *
     * @param id3Codec codec para leitura de metadados ID3
     * @param coverSearcher buscador de capas de álbuns
     */
    public CoverService(Id3Codec id3Codec, AlbumCoverSearcher coverSearcher) {
        this.id3Codec = id3Codec;
        this.coverSearcher = coverSearcher;
    }

    /**
     * Baixa a capa do álbum na pasta do arquivo e retorna o caminho salvo.
     *
     * @param musicPath caminho absoluto do arquivo MP3
     * @return caminho do arquivo de capa salvo
     * @throws IOException se a pasta não for encontrada ou nenhuma imagem for encontrada
     */
    public String download(String musicPath) throws IOException {
        MusicFile musicFile = id3Codec.read(musicPath);
        String query = buildQueryArtistAlbum(musicFile);

        CoverImage image = coverSearcher.findCover(query);
        if (image == null || image.isEmpty()) {
            throw new IOException("Nenhuma imagem encontrada para \"" + query + "\"");
        }

        Path folder = Paths.get(musicPath).toAbsolutePath().getParent();
        if (folder == null) {
            throw new IOException("Pasta do arquivo não encontrada");
        }
        Path target = folder.resolve("cover." + extensionFor(image.contentType()));
        Files.write(target, image.bytes());
        log.info("[Capa] Capa salva em {}", target);
        return target.toString();
    }

    /**
     * Monta o termo de busca "artista + álbum" a partir dos metadados ID3.
     *
     * @param musicFile agregado de música com os metadados
     * @return termo de busca para a API de capas
     */
    private static String buildQueryArtistAlbum(MusicFile musicFile) {
        String artist = blank(musicFile.getMetadata().artist());
        String album = blank(musicFile.getMetadata().album());
        if (artist.isEmpty() && album.isEmpty()) {
            artist = MusicFileNaming.artistFromFilename(musicFile.getPath());
        }
        if (album.isEmpty() || album.equalsIgnoreCase(artist)) {
            return artist;
        }
        return artist + " " + album;
    }

    /**
     * Mapeia o content-type para a extensão de arquivo salva.
     *
     * @param contentType content-type da imagem
     * @return extensão do arquivo (jpg, png, webp, gif)
     */
    private static String extensionFor(String contentType) {
        if (contentType == null) return "jpg";
        String ct = contentType.toLowerCase();
        if (ct.contains("png")) return "png";
        if (ct.contains("webp")) return "webp";
        if (ct.contains("gif")) return "gif";
        return "jpg";
    }

    /**
     * Retorna a string vazia se o valor for nulo.
     *
     * @param s string a ser verificada
     * @return string original ou vazia
     */
    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}
