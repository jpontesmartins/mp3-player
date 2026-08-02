package com.mp3player;

import com.mp3player.application.lyrics.LyricsAppService;
import com.mp3player.application.metadata.Id3AppService;
import com.mp3player.application.playlist.PlaylistAppService;
import com.mp3player.application.player.PlayerService;
import com.mp3player.controller.LyricsController;
import com.mp3player.controller.MetadataController;
import com.mp3player.controller.PlayerController;
import com.mp3player.controller.PlaylistController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class Mp3PlayerApplicationTests {

    @Autowired
    PlayerController playerController;
    @Autowired
    PlaylistController playlistController;
    @Autowired
    LyricsController lyricsController;
    @Autowired
    MetadataController metadataController;
    @Autowired
    PlayerService playerService;
    @Autowired
    PlaylistAppService playlistAppService;
    @Autowired
    LyricsAppService lyricsAppService;
    @Autowired
    Id3AppService id3AppService;

    @Test
    void contextLoads() {
        assertNotNull(playerController);
        assertNotNull(playlistController);
        assertNotNull(lyricsController);
        assertNotNull(metadataController);
        assertNotNull(playerService);
        assertNotNull(playlistAppService);
        assertNotNull(lyricsAppService);
        assertNotNull(id3AppService);
    }
}