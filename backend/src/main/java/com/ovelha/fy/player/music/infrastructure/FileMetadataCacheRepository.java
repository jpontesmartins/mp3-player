package com.ovelha.fy.player.music.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ovelha.fy.player.music.domain.repository.MetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação baseada em arquivo de {@link MetadataCacheRepository}. As tags
 * ficam em um único JSON na mesma pasta do log do backend, mantidas em memória
 * e gravadas de forma atômica (arquivo temporário + move) a cada alteração.
 */
@Repository
public class FileMetadataCacheRepository implements MetadataCacheRepository {

    private static final Logger log = LoggerFactory.getLogger(FileMetadataCacheRepository.class);

    private final Path cacheFile;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, Map<String, String>> entries = new ConcurrentHashMap<>();

    /**
     * Construtor do repositório de cache de metadados.
     *
     * @param logFile caminho do arquivo de log do backend
     */
    @Autowired
    public FileMetadataCacheRepository(@Value("${mp3.log-file:}") String logFile) {
        this(cacheFileFor(logFile));
    }

    FileMetadataCacheRepository(Path cacheFile) {
        this(cacheFile, new ObjectMapper());
    }

    FileMetadataCacheRepository(Path cacheFile, ObjectMapper objectMapper) {
        this.cacheFile = cacheFile;
        this.objectMapper = objectMapper;
        load();
    }

    /**
     * Calcula o caminho do arquivo de cache baseado na localização do log.
     *
     * @param logFile caminho do arquivo de log
     * @return caminho do arquivo de cache
     */
    private static Path cacheFileFor(String logFile) {
        if (logFile != null && !logFile.isBlank()) {
            Path parent = Paths.get(logFile).getParent();
            if (parent != null) {
                return parent.resolve("music-cache.json");
            }
        }
        return Paths.get(System.getProperty("user.home"), ".mp3-player", "music-cache.json");
    }

    /**
     * Retorna as tags em cache para o caminho informado.
     *
     * @param path caminho do arquivo
     * @return mapa de tags, ou {@code null} se não estiver em cache
     */
    @Override
    public Map<String, String> get(String path) {
        return entries.get(path);
    }

    /**
     * Armazena as tags do arquivo no cache e persiste no disco.
     *
     * @param path caminho do arquivo
     * @param tags mapa de tags a serem armazenadas
     */
    @Override
    public void put(String path, Map<String, String> tags) {
        entries.put(path, tags);
        persist();
    }

    /**
     * Armazena várias entradas de uma vez e persiste no disco.
     *
     * @param tagsByPath mapa de caminho → tags
     */
    @Override
    public void putAll(Map<String, Map<String, String>> tagsByPath) {
        if (tagsByPath.isEmpty()) return;
        entries.putAll(tagsByPath);
        persist();
    }

    /**
     * Retorna a localização do arquivo onde o cache é persistido.
     *
     * @return caminho do arquivo de cache
     */
    @Override
    public String location() {
        return cacheFile.toString();
    }

    /**
     * Carrega o cache do arquivo JSON na inicialização.
     */
    private void load() {
        if (!Files.exists(cacheFile)) return;
        try {
            String json = Files.readString(cacheFile, StandardCharsets.UTF_8);
            Map<String, Map<String, String>> loaded = objectMapper.readValue(json, new TypeReference<>() { });
            if (loaded != null && !loaded.isEmpty()) {
                entries.putAll(loaded);
            }
            log.info("[Cache] {} metadados carregados de {}", entries.size(), cacheFile);
        } catch (IOException e) {
            log.warn("[Cache] Não foi possível carregar o cache de metadados {}: {}", cacheFile, e.getMessage());
        }
    }

    /**
     * Persiste o cache em disco de forma atômica (arquivo temporário + move).
     */
    private void persist() {
        try {
            Files.createDirectories(cacheFile.getParent());
            Path tmp = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
            String json = objectMapper.writeValueAsString(entries);
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            Files.move(tmp, cacheFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("[Cache] Cache de metadados atualizado ({} entradas)", entries.size());
        } catch (IOException e) {
            log.error("[Cache] Não foi possível gravar o cache de metadados", e);
        }
    }
}
