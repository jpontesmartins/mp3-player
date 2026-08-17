package com.mp3player.infrastructure.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import com.mp3player.domain.repository.MetadataCacheRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Decorator que adiciona cache transparente a qualquer implementação de {@link Id3Codec}.
 * Verifica o {@link MetadataCacheRepository} antes de delegar para o codec envolvido.
 */
public class CachedId3Codec implements Id3Codec {

    private static final Logger log = LoggerFactory.getLogger(CachedId3Codec.class);

    private final Id3Codec delegate;
    private final MetadataCacheRepository cache;

    public CachedId3Codec(Id3Codec delegate, MetadataCacheRepository cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Music read(String filePath) {
        Map<String, String> cached = cache.get(filePath);
        if (cached != null) {
            log.info("[ID3] Servido do cache: {}", filePath);
            return new Music(filePath, Music.Metadata.fromTags(cached));
        }
        Music music = delegate.read(filePath);
        cache.put(filePath, music.toTagMap());
        return music;
    }

    @Override
    public Music update(String filePath, Map<String, String> tags) {
        Music updated = delegate.update(filePath, tags);
        cache.put(filePath, updated.toTagMap());
        log.info("[ID3] Cache atualizado para: {}", filePath);
        return updated;
    }
}
