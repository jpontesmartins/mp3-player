package com.mp3player;

import com.mp3player.lyrics.application.LyricsService;
import com.mp3player.metadata.application.Id3Service;
import com.mp3player.playlist.application.PlaylistService;
import com.mp3player.player.application.PlayerService;
import com.mp3player.lyrics.web.LyricsController;
import com.mp3player.metadata.web.MetadataController;
import com.mp3player.player.web.PlayerController;
import com.mp3player.playlist.web.PlaylistController;
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
    PlaylistService playlistAppService;
    @Autowired
    LyricsService lyricsAppService;
    @Autowired
    Id3Service id3AppService;

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