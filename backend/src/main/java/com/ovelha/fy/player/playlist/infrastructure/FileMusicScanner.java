package com.ovelha.fy.player.playlist.infrastructure;

import com.ovelha.fy.player.domain.model.MusicFile;
import com.ovelha.fy.player.playlist.domain.port.MusicScanner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Implementação do port {@link MusicScanner} que percorre uma pasta recursivamente
 * e retorna as músicas (.mp3) encontradas.
 */
@Component
public class FileMusicScanner implements MusicScanner {

    /**
     * Escaneia uma pasta recursivamente e retorna as músicas (.mp3) encontradas.
     *
     * @param folderPath caminho absoluto da pasta a ser escaneada.
     * @return lista de músicas encontradas.
     * @throws IOException se a pasta não existir, não for um diretório ou não puder ser lida.
     */
    @Override
    public List<MusicFile> scanFolder(String folderPath) throws IOException {
        Path start = Paths.get(folderPath);
        if (!Files.exists(start) || !Files.isDirectory(start)) {
            throw new IOException("Directory not found: " + folderPath);
        }
        try (Stream<Path> stream = Files.walk(start)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".mp3"))
                    .map(Path::toAbsolutePath)
                    .map(p -> new MusicFile(p.toString(), MusicFile.Metadata.empty()))
                    .toList();
        }
    }
}