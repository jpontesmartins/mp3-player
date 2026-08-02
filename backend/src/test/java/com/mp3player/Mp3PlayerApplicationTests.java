package com.mp3player;

import com.mp3player.application.lyrics.LyricsAppService;
import com.mp3player.application.metadata.Id3AppService;
import com.mp3player.application.playlist.PlaylistAppService;
import com.mp3player.controller.PlayController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class Mp3PlayerApplicationTests {

    @Autowired
    PlayController playController;
    @Autowired
    PlaylistAppService playlistAppService;
    @Autowired
    LyricsAppService lyricsAppService;
    @Autowired
    Id3AppService id3AppService;

    @Test
    void contextLoads() {
        assertNotNull(playController);
        assertNotNull(playlistAppService);
        assertNotNull(lyricsAppService);
        assertNotNull(id3AppService);
    }
}