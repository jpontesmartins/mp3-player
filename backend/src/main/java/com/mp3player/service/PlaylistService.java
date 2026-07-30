package com.mp3player.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

@Service
public class PlaylistService {

    public List<String> scanFolder(String folderPath) throws IOException {
        Path start = Paths.get(folderPath);
        if (!Files.exists(start) || !Files.isDirectory(start)) {
            throw new IOException("Directory not found: " + folderPath);
        }
        try (Stream<Path> stream = Files.walk(start)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".mp3"))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();
        }
    }
}
