package com.mp3player.controller;

import com.mp3player.application.metadata.Id3AppService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Adaptador HTTP para o módulo de metadados: leitura/edição de tags ID3 e
 * fornecimento da capa do álbum. Apenas traduz requisições web em chamadas ao
 * {@link Id3AppService}.
 */
@RestController
public class MetadataController {

    private final Id3AppService id3Service;

    public MetadataController(Id3AppService id3Service) {
        this.id3Service = id3Service;
    }

    @GetMapping("/id3")
    public ResponseEntity<Map<String, String>> getId3(@RequestParam String path) {
        return ResponseEntity.ok(id3Service.getForFile(path));
    }

    @PostMapping("/id3/update")
    public ResponseEntity<?> updateId3(@RequestBody Id3UpdateRequest request) {
        try {
            return ResponseEntity.ok(id3Service.update(request.path(), request.tags()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    public record Id3UpdateRequest(String path, Map<String, String> tags) {}

    @PostMapping("/id3/bulk")
    public ResponseEntity<Map<String, Map<String, String>>> getBulkId3(@RequestBody List<String> paths) {
        return ResponseEntity.ok(id3Service.getBulk(paths));
    }

    @GetMapping("/cover")
    public ResponseEntity<Resource> getCover(@RequestParam String path) {
        try {
            Path parent = Paths.get(path).getParent();
            if (parent == null) return ResponseEntity.notFound().build();
            String[] coverNames = { "cover", "folder", "album", "front", "art", "artwork" };
            String[] extensions = { "jpg", "jpeg", "png" };
            try (var files = Files.list(parent)) {
                var coverFile = files
                        .filter(f -> {
                            if (!Files.isRegularFile(f)) return false;
                            String name = f.getFileName().toString().toLowerCase();
                            int dot = name.lastIndexOf('.');
                            if (dot < 0) return false;
                            String stem = name.substring(0, dot);
                            String ext = name.substring(dot + 1);
                            if (!Arrays.asList(extensions).contains(ext)) return false;
                            return Arrays.asList(coverNames).contains(stem);
                        })
                        .findFirst();
                if (coverFile.isPresent()) {
                    var file = coverFile.get();
                    var resource = new InputStreamResource(Files.newInputStream(file));
                    String name = file.getFileName().toString().toLowerCase();
                    String contentType = name.endsWith("png") ? "image/png" : "image/jpeg";
                    return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
                }
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}