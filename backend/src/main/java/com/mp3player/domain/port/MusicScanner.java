package com.mp3player.domain.port;

import com.mp3player.domain.model.Music;

import java.io.IOException;
import java.util.List;

/**
 * Port for scanning a physical folder and returning every music found in it,
 * recursively. This is the source for the "physical playlist" and the library.
 */
public interface MusicScanner {

    List<Music> scanFolder(String folderPath) throws IOException;
}