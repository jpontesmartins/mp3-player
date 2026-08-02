package com.mp3player.domain.port;

import com.mp3player.domain.model.Music;

import java.util.Map;

/**
 * Port for reading and writing the ID3 metadata of a music file.
 * Implementations may use mp3agic or another codec.
 */
public interface Id3Codec {

    /** Reads the metadata of the file at the given path into a Music aggregate. */
    Music read(String filePath);

    /** Updates the editable fields and returns the updated {@link Music}. */
    Music update(String filePath, Map<String, String> tags);
}