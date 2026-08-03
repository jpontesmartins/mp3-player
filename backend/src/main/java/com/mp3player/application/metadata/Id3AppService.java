package com.mp3player.application.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service da aplicação para o módulo de edição de ID3: lê um arquivo, lê vários
 * de uma vez (bulk) e atualiza as tags editáveis.
 */
@Service
public class Id3AppService {

    private final Id3Codec id3Codec;

    public Id3AppService(Id3Codec id3Codec) {
        this.id3Codec = id3Codec;
    }

    /** Lê as tags ID3 de um único arquivo como mapa de troca (wire). */
    public Map<String, String> getForFile(String filePath) {
        try {
            return id3Codec.read(filePath).toTagMap();
        } catch (Exception e) {
            return Map.of("error", "Could not read ID3 tags");
        }
    }

    /** Lê as tags ID3 de vários arquivos de uma vez, indexando pelo caminho. */
    public Map<String, Map<String, String>> getBulk(List<String> paths) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String path : paths) {
            result.put(path, getForFile(path));
        }
        return result;
    }

    /** Atualiza as tags editáveis do arquivo e retorna as tags resultantes. */
    public Map<String, String> update(String filePath, Map<String, String> tags) {
        Music updated = id3Codec.update(filePath, tags);
        return updated.toTagMap();
    }
}