package com.ovelha.fy.shared.config;

import com.ovelha.fy.player.music.domain.port.Id3Codec;
import com.ovelha.fy.player.music.domain.repository.MetadataCacheRepository;
import com.ovelha.fy.player.music.infrastructure.CachedId3Codec;
import com.ovelha.fy.player.music.infrastructure.Id3MagicCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do bean responsável pela leitura e escrita de metadados ID3.
 */
@Configuration
public class Id3CodecConfig {

    /**
     * Cria o bean {@link Id3Codec} com cache, delegando a operação
     * real para o {@link Id3MagicCodec}.
     *
     * @param delegate codec ID3 subjacente que realiza a leitura/escrita
     * @param cache    repositório de cache de metadados
     * @return instância de {@link Id3Codec} com suporte a cache
     */
    @Bean
    public Id3Codec id3Codec(Id3MagicCodec delegate, MetadataCacheRepository cache) {
        return new CachedId3Codec(delegate, cache);
    }
}
