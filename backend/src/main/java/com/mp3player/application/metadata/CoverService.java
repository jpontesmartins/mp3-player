package com.mp3player.application.metadata;

import com.mp3player.domain.model.CoverImage;
import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.AlbumCoverSearcher;
import com.mp3player.domain.port.Id3Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mp3player.domain.util.MusicFileNaming;
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

    public CoverService(Id3Codec id3Codec, AlbumCoverSearcher coverSearcher) {
        this.id3Codec = id3Codec;
        this.coverSearcher = coverSearcher;
    }

    /** Baixa a capa do álbum na pasta do arquivo e retorna o caminho salvo. */
    public String download(String musicPath) throws IOException {
        Music music = id3Codec.read(musicPath);
        String query = buildQuery(music);

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

    /** Monta o termo de busca "artista + álbum" a partir dos metadados ID3. */
    private static String buildQuery(Music music) {
        String artist = blank(music.getMetadata().getArtist());
        String album = blank(music.getMetadata().getAlbum());
        if (artist.isEmpty() && album.isEmpty()) {
            artist = MusicFileNaming.artistFromFilename(music.getPath());
        }
        if (album.isEmpty() || album.equalsIgnoreCase(artist)) {
            return artist;
        }
        return artist + " " + album;
    }

    /** Mapeia o content-type para a extensão salva; JPEG é o padrão. */
    private static String extensionFor(String contentType) {
        if (contentType == null) return "jpg";
        String ct = contentType.toLowerCase();
        if (ct.contains("png")) return "png";
        if (ct.contains("webp")) return "webp";
        if (ct.contains("gif")) return "gif";
        return "jpg";
    }

    private static String blank(String s) {
        return s == null ? "" : s.trim();
    }
}