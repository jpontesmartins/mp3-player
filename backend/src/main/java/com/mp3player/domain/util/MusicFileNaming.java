package com.mp3player.domain.util;

import java.nio.file.Paths;

/**
 * Utilitário para extração de informações de nomes de arquivos de música.
 * Fornece métodos para obter o nome base (sem extensão), artista e título
 * a partir do convênio "Artista - Música.mp3".
 */
public final class MusicFileNaming {

    private MusicFileNaming() {
    }

    /**
     * Retorna o nome do arquivo sem o caminho completo e sem a extensão {@code .mp3}.
     *
     * @param path caminho completo do arquivo
     * @return nome base do arquivo
     */
    public static String baseName(String path) {
        String name = Paths.get(path).getFileName().toString();
        if (name.toLowerCase().endsWith(".mp3")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    /**
     * Extrai o nome do artista a partir do nome do arquivo no formato "Artista - Música.mp3".
     * Retorna string vazia quando o formato não contém o separador " - ".
     *
     * @param path caminho completo do arquivo
     * @return nome do artista ou string vazia
     */
    public static String artistFromFilename(String path) {
        String base = baseName(path);
        int dash = base.indexOf(" - ");
        return dash > 0 ? base.substring(0, dash).trim() : "";
    }

    /**
     * Extrai o título da música a partir do nome do arquivo no formato "Artista - Música.mp3".
     * Quando o formato não contém o separador " - ", retorna o nome base completo.
     *
     * @param path caminho completo do arquivo
     * @return título da música
     */
    public static String titleFromFilename(String path) {
        String base = baseName(path);
        int dash = base.indexOf(" - ");
        return dash > 0 ? base.substring(dash + 3).trim() : base.trim();
    }
}
