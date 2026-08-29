package com.mp3player.metadata.infrastructure;

import com.mp3player.player.domain.model.MusicFile;
import com.mp3player.metadata.domain.port.Id3Codec;
import com.mp3player.metadata.domain.repository.MetadataCacheRepository;
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

    /**
     * Construtor do codec com cache transparente.
     *
     * @param delegate codec envolvido para leitura e escrita de metadados
     * @param cache repositório de cache de metadados
     */
    public CachedId3Codec(Id3Codec delegate, MetadataCacheRepository cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    /**
     * Lê os metadados do arquivo, verificando o cache antes de delegar para o codec.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @return agregado de música com os metadados lidos
     */
    @Override
    public MusicFile read(String filePath) {
        Map<String, String> cached = cache.get(filePath);
        if (cached != null && !isStale(filePath, cached)) {
            log.info("[ID3] Servido do cache: {}", filePath);
            return new MusicFile(filePath, MusicFile.Metadata.fromTags(cached));
        }
        MusicFile musicFile = delegate.read(filePath);
        putWithTimestamp(filePath, musicFile.toTagMap());
        return musicFile;
    }

    /**
     * Atualiza as tags editáveis do arquivo e atualiza o cache.
     *
     * @param filePath caminho absoluto do arquivo MP3
     * @param tags mapa de tags a serem atualizadas
     * @return agregado de música com os metadados atualizados
     */
    @Override
    public MusicFile update(String filePath, Map<String, String> tags) {
        MusicFile updated = delegate.update(filePath, tags);
        putWithTimestamp(filePath, updated.toTagMap());
        log.info("[ID3] Cache atualizado para: {}", filePath);
        return updated;
    }

    /**
     * Verifica se o cache está desatualizado comparando o timestamp de modificação.
     *
     * @param filePath caminho do arquivo
     * @param cached tags em cache
     * @return {@code true} se o cache estiver desatualizado
     */
    private boolean isStale(String filePath, Map<String, String> cached) {
        String stored = cached.get(LAST_MODIFIED_KEY);
        if (stored == null) return true;
        long current = lastModified(filePath);
        return current != Long.parseLong(stored);
    }

    /**
     * Armazena as tags no cache com o timestamp de modificação atual.
     *
     * @param filePath caminho do arquivo
     * @param tags tags a serem armazenadas
     */
    private void putWithTimestamp(String filePath, Map<String, String> tags) {
        Map<String, String> toStore = new HashMap<>(tags);
        toStore.put(LAST_MODIFIED_KEY, String.valueOf(lastModified(filePath)));
        cache.put(filePath, toStore);
    }

    /**
     * Retorna o timestamp de última modificação do arquivo.
     *
     * @param filePath caminho do arquivo
     * @return timestamp de última modificação em milissegundos
     */
    private long lastModified(String filePath) {
        return Path.of(filePath).toFile().lastModified();
    }
}
