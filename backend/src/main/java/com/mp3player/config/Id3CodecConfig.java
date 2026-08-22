package com.mp3player.config;

import com.mp3player.metadata.domain.port.Id3Codec;
import com.mp3player.metadata.domain.repository.MetadataCacheRepository;
import com.mp3player.metadata.infrastructure.CachedId3Codec;
import com.mp3player.metadata.infrastructure.Id3MagicCodec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Id3CodecConfig {

    @Bean
    public Id3Codec id3Codec(Id3MagicCodec delegate, MetadataCacheRepository cache) {
        return new CachedId3Codec(delegate, cache);
    }
}
