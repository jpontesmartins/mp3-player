package com.ovelha.fy;

import com.ovelha.fy.lyrics.application.LyricsService;
import com.ovelha.fy.player.music.application.Id3Service;
import com.ovelha.fy.player.playlist.application.PlaylistService;
import com.ovelha.fy.player.controls.application.PlayerService;
import com.ovelha.fy.lyrics.web.LyricsController;
import com.ovelha.fy.player.music.web.MetadataController;
import com.ovelha.fy.player.controls.web.PlayerController;
import com.ovelha.fy.player.playlist.web.PlaylistController;
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