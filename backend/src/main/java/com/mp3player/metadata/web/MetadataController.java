package com.mp3player.metadata.web;

import com.mp3player.metadata.application.CoverService;
import com.mp3player.metadata.application.Id3Service;
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
 * {@link Id3Service}.
 */
@RestController
public class MetadataController {

    private final Id3Service id3Service;
    private final CoverService coverService;

    /**
     * Construtor do controlador de metadados.
     *
     * @param id3Service serviço de metadados ID3
     * @param coverService serviço de download de capas
     */
    public MetadataController(Id3Service id3Service, CoverService coverService) {
        this.id3Service = id3Service;
        this.coverService = coverService;
    }

    /**
     * Retorna as tags ID3 de um único arquivo.
     *
     * @param path caminho absoluto do arquivo MP3
     * @return mapa de tags do arquivo
     */
    @GetMapping("/id3")
    public ResponseEntity<Map<String, String>> getId3(@RequestParam String path) {
        return ResponseEntity.ok(id3Service.getForFile(path));
    }

    /**
     * Atualiza as tags ID3 de um arquivo.
     *
     * @param request requisição com caminho e tags a serem atualizadas
     * @return tags atualizadas ou mensagem de erro
     */
    @PostMapping("/id3/update")
    public ResponseEntity<?> updateId3(@RequestBody Id3UpdateRequest request) {
        try {
            return ResponseEntity.ok(id3Service.update(request.path(), request.tags()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Requisição de atualização de tags ID3.
     *
     * @param path caminho do arquivo
     * @param tags mapa de tags a serem atualizadas
     */
    public record Id3UpdateRequest(String path, Map<String, String> tags) {}

    /**
     * Retorna as tags ID3 de vários arquivos de uma vez.
     *
     * @param paths lista de caminhos dos arquivos MP3
     * @param refresh se {@code true}, relê todos os arquivos ignorando o cache
     * @return mapa de caminho → tags de cada arquivo
     */
    @PostMapping("/id3/bulk")
    public ResponseEntity<Map<String, Map<String, String>>> getBulkId3(
            @RequestBody List<String> paths,
            @RequestParam(defaultValue = "false") boolean refresh) {
        return ResponseEntity.ok(id3Service.getBulk(paths, refresh));
    }

    /**
     * Retorna a imagem de capa do álbum associado ao arquivo informado.
     * Busca por arquivos com nomes como "cover.jpg", "folder.png", etc.
     *
     * @param path caminho absoluto do arquivo MP3
     * @return recurso da imagem de capa ou 404 se não encontrada
     */
    @GetMapping("/cover")
    public ResponseEntity<Resource> getCover(@RequestParam String path) {
        try {
            Path parent = Paths.get(path).getParent();
            if (parent == null) return ResponseEntity.notFound().build();
            String[] coverNames = { "cover", "folder", "album", "front", "art", "artwork" };
            String[] extensions = { "jpg", "jpeg", "png", "webp", "gif" };
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
                    String contentType = contentTypeFor(name);
                    return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).body(resource);
                }
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retorna o content-type baseado na extensão do arquivo.
     *
     * @param name nome do arquivo
     * @return content-type correspondente
     */
    /**
     * Retorna o content-type baseado na extensão do arquivo.
     *
     * @param name nome do arquivo
     * @return content-type correspondente
     */
    private static String contentTypeFor(String name) {
        if (name.endsWith("png")) return "image/png";
        if (name.endsWith("webp")) return "image/webp";
        if (name.endsWith("gif")) return "image/gif";
        return "image/jpeg";
    }

    /**
     * Requisição de download de capa.
     *
     * @param path caminho do arquivo MP3
     */
    public record CoverDownloadRequest(String path) {}

    /**
     * Baixa a capa do álbum a partir da web e salva na pasta do arquivo.
     *
     * @param request requisição com o caminho do arquivo MP3
     * @return caminho do arquivo de capa salvo ou mensagem de erro
     */
    @PostMapping("/cover/download")
    public ResponseEntity<String> downloadCover(@RequestBody CoverDownloadRequest request) {
        if (request.path() == null || request.path().isBlank()) {
            return ResponseEntity.badRequest().body("Caminho é obrigatório");
        }
        try {
            String saved = coverService.download(request.path());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erro ao baixar capa: " + e.getMessage());
        }
    }
}