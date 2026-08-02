package com.mp3player.infrastructure.music;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.MusicScanner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileMusicScanner implements MusicScanner {

    @Override
    public List<Music> scanFolder(String folderPath) throws IOException {
        Path start = Paths.get(folderPath);
        if (!Files.exists(start) || !Files.isDirectory(start)) {
            throw new IOException("Directory not found: " + folderPath);
        }
        try (Stream<Path> stream = Files.walk(start)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".mp3"))
                    .map(Path::toAbsolutePath)
                    .map(p -> new Music(p.toString(), Music.Metadata.empty()))
                    .toList();
        }
    }
}