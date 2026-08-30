package com.ovelha.fy.player.music.web;

import com.ovelha.fy.player.music.application.Id3Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Adaptador HTTP para informações de execução da aplicação: local do log do
 * backend, local do cache de metadados e portas usadas pelo backend e frontend.
 */
@RestController
public class InfoController {

    private static final Logger log = LoggerFactory.getLogger(InfoController.class);

    private final String logFile;
    private final String backendPort;
    private final String frontendPort;
    private final Id3Service id3Service;

    /**
     * Construtor do controlador de informações.
     *
     * @param logFile caminho do arquivo de log do backend
     * @param backendPort porta do backend
     * @param frontendPort porta do frontend
     * @param id3Service serviço de metadados ID3
     */
    public InfoController(
            @Value("${mp3.log-file:}") String logFile,
            @Value("${server.port:8111}") String backendPort,
            @Value("${mp3.frontend-port:8112}") String frontendPort,
            Id3Service id3Service) {
        this.logFile = logFile;
        this.backendPort = backendPort;
        this.frontendPort = frontendPort;
        this.id3Service = id3Service;
    }

    /**
     * Retorna informações de execução da aplicação (log, cache, portas).
     *
     * @return mapa com as informações de configuração
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getInfo() {
        log.info("GET /info: logFile={}, backend={}, frontend={}", logFile, backendPort, frontendPort);
        Map<String, String> info = new LinkedHashMap<>();
        info.put("logFile", logFile);
        info.put("cacheFile", id3Service.cacheLocation());
        info.put("backendPort", backendPort);
        info.put("frontendPort", frontendPort);
        return ResponseEntity.ok(info);
    }
}
