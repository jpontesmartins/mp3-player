package com.mp3player.infrastructure.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.repository.MetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Decorator que adiciona cache transparente a qualquer implementação de {@link Id3Codec}.
 * Verifica o {@link MetadataCacheRepository} antes de delegar para o codec envolvido.
 * Valida o timestamp de modificação do arquivo para detectar mudanças externas.
 */
public class CachedId3Codec implements Id3Codec {

    private static final Logger log = LoggerFactory.getLogger(CachedId3Codec.class);
    private static final String LAST_MODIFIED_KEY = "_lastModified";

    private final Id3Codec delegate;
    private final MetadataCacheRepository cache;

    public CachedId3Codec(Id3Codec delegate, MetadataCacheRepository cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Music read(String filePath) {
        Map<String, String> cached = cache.get(filePath);
        if (cached != null && !isStale(filePath, cached)) {
            log.info("[ID3] Servido do cache: {}", filePath);
            return new Music(filePath, Music.Metadata.fromTags(cached));
        }
        Music music = delegate.read(filePath);
        putWithTimestamp(filePath, music.toTagMap());
        return music;
    }

    @Override
    public Music update(String filePath, Map<String, String> tags) {
        Music updated = delegate.update(filePath, tags);
        putWithTimestamp(filePath, updated.toTagMap());
        log.info("[ID3] Cache atualizado para: {}", filePath);
        return updated;
    }

    private boolean isStale(String filePath, Map<String, String> cached) {
        String stored = cached.get(LAST_MODIFIED_KEY);
        if (stored == null) return true;
        long current = lastModified(filePath);
        return current != Long.parseLong(stored);
    }

    private void putWithTimestamp(String filePath, Map<String, String> tags) {
        Map<String, String> toStore = new HashMap<>(tags);
        toStore.put(LAST_MODIFIED_KEY, String.valueOf(lastModified(filePath)));
        cache.put(filePath, toStore);
    }

    private long lastModified(String filePath) {
        return Path.of(filePath).toFile().lastModified();
    }
}
