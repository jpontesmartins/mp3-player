package com.mp3player.application.metadata;

import com.mp3player.domain.model.Music;
import com.mp3player.domain.port.Id3Codec;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service for the ID3 editing module: reads a single file, reads many
 * files at once (bulk) and updates the editable tags.
 */
@Service
public class Id3AppService {

    private final Id3Codec id3Codec;

    public Id3AppService(Id3Codec id3Codec) {
        this.id3Codec = id3Codec;
    }

    public Map<String, String> getForFile(String filePath) {
        try {
            return id3Codec.read(filePath).toTagMap();
        } catch (Exception e) {
            return Map.of("error", "Could not read ID3 tags");
        }
    }

    public Map<String, Map<String, String>> getBulk(List<String> paths) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (String path : paths) {
            result.put(path, getForFile(path));
        }
        return result;
    }

    public Map<String, String> update(String filePath, Map<String, String> tags) {
        Music updated = id3Codec.update(filePath, tags);
        return updated.toTagMap();
    }
}