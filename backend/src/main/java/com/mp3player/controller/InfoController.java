package com.mp3player.controller;

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
 * backend e portas usadas pelo backend e pelo frontend.
 */
@RestController
public class InfoController {

    private static final Logger log = LoggerFactory.getLogger(InfoController.class);

    private final String logFile;
    private final String backendPort;
    private final String frontendPort;

    public InfoController(
            @Value("${mp3.log-file:}") String logFile,
            @Value("${server.port:8111}") String backendPort,
            @Value("${mp3.frontend-port:8112}") String frontendPort) {
        this.logFile = logFile;
        this.backendPort = backendPort;
        this.frontendPort = frontendPort;
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getInfo() {
        log.info("GET /info: logFile={}, backend={}, frontend={}", logFile, backendPort, frontendPort);
        Map<String, String> info = new LinkedHashMap<>();
        info.put("logFile", logFile);
        info.put("backendPort", backendPort);
        info.put("frontendPort", frontendPort);
        return ResponseEntity.ok(info);
    }
}
