package com.mp3player.application.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.repository.MetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service da aplicação para o módulo de edição de ID3: lê um arquivo, lê vários
 * de uma vez (bulk) e atualiza as tags editáveis.
 */
@Service
public class Id3Service {

    private static final Logger log = LoggerFactory.getLogger(Id3Service.class);

    private final Id3Codec id3Codec;
    private final MetadataCacheRepository cache;

    public Id3Service(Id3Codec id3Codec, MetadataCacheRepository cache) {
        this.id3Codec = id3Codec;
        this.cache = cache;
    }

    /** Lê as tags ID3 de um único arquivo como mapa de troca (wire). */
    public Map<String, String> getForFile(String filePath) {
        Map<String, String> cached = cache.get(filePath);
        if (cached != null) {
            log.info("[ID3] Servido do cache: {}", filePath);
            return cached;
        }
        Map<String, String> tags = readForFile(filePath);
        cache.put(filePath, tags);
        return tags;
    }

    /**
     * Lê as tags ID3 de vários arquivos de uma vez, indexando pelo caminho.
     * Sem {@code refresh}, serve os arquivos já em cache e lê apenas os
     * faltantes; com {@code refresh}, relê todos e sobrescreve o cache. Cada
     * arquivo é lido em uma virtual thread.
     */
    public Map<String, Map<String, String>> getBulk(List<String> paths, boolean refresh) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        List<String> toRead = new ArrayList<>(paths.size());
        int cachedCount = 0;
        if (refresh) {
            toRead.addAll(paths);
        } else {
            for (String path : paths) {
                Map<String, String> cached = cache.get(path);
                if (cached != null) {
                    result.put(path, cached);
                    cachedCount++;
                } else {
                    toRead.add(path);
                }
            }
        }
        if (!toRead.isEmpty()) {
            Map<String, Map<String, String>> read = readBulk(toRead);
            result.putAll(read);
            cache.putAll(read);
            log.info("[ID3] Bulk: {} do cache, {} processados dos arquivos", cachedCount, toRead.size());
        } else {
            log.info("[ID3] Bulk: {} arquivos servidos do cache", cachedCount);
        }
        return result;
    }

    /** Lê as tags de um lote de arquivos, um por virtual thread. */
    private Map<String, Map<String, String>> readBulk(List<String> paths) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        List<Future<Map<String, String>>> futures = new ArrayList<>(paths.size());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (String path : paths) {
                futures.add(executor.submit(() -> readForFile(path)));
            }
            for (int i = 0; i < futures.size(); i++) {
                try {
                    result.put(paths.get(i), futures.get(i).get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    // nunca acontece: readForFile captura qualquer exceção interna
                }
            }
        }
        return result;
    }

    private Map<String, String> readForFile(String filePath) {
        log.info("[ID3] Processando arquivo: {}", filePath);
        try {
            return id3Codec.read(filePath).toTagMap();
        } catch (Exception e) {
            return Map.of("error", "Could not read ID3 tags");
        }
    }

    /** Atualiza as tags editáveis do arquivo e retorna as tags resultantes. */
    public Map<String, String> update(String filePath, Map<String, String> tags) {
        Music updated = id3Codec.update(filePath, tags);
        Map<String, String> result = updated.toTagMap();
        cache.put(filePath, result);
        log.info("[ID3] Cache atualizado para: {}", filePath);
        return result;
    }

    /** Localização do arquivo onde o cache de ID3 é persistido. */
    public String cacheLocation() {
        return cache.location();
    }
}