package com.ovelha.fy.lyrics.domain.model;

import com.ovelha.fy.shared.domain.util.MusicFileNaming;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Gera a lista ordenada de nomes de artista a tentar ao buscar uma letra.
 * O artista da tag ID3 vem primeiro; em seguida os artistas inferidos do nome do
 * arquivo (ex.: {@code 1-11 Gilberto Gil e Os Mutantes - Bat Macumba.mp3}).
 */
public final class ArtistCandidates {

    /** Limite de candidatos, para não multiplicar requisições externas. */
    static final int MAX = 4;

    private static final Pattern TRACK_PREFIX = Pattern.compile(
            "^(?:0\\d+[\\s.-]+|\\d+[-.]\\d+[\\s.-]+|\\d+\\.\\s+)");
    private static final Pattern SEPARATORS = Pattern.compile(
            "\\s*,\\s*|\\s+e\\s+|\\s+&\\s+|\\s+(?:feat\\.?|ft\\.?|part\\.?)\\s+|\\s+x\\s+",
            Pattern.CASE_INSENSITIVE);

    private ArtistCandidates() {
    }

    /**
     * Monta os candidatos de artista, sem vazios nem duplicatas (ignorando caixa e acentos).
     *
     * @param tagArtist artista da tag ID3 (pode ser {@code null})
     * @param musicPath caminho absoluto do arquivo de áudio
     * @return lista de até {@value #MAX} candidatos, tag primeiro
     */
    public static List<String> from(String tagArtist, String musicPath) {
        List<String> result = new ArrayList<>();
        add(result, tagArtist);

        String fromFile = MusicFileNaming.artistFromFilename(musicPath);
        fromFile = TRACK_PREFIX.matcher(fromFile).replaceFirst("").trim();
        // "05 - Título.mp3": o "artista" do arquivo é só o número da faixa
        if (fromFile.isEmpty() || fromFile.chars().allMatch(Character::isDigit)) {
            return result;
        }
        // A tag já traz o nome completo (ex.: "Earth, Wind & Fire"): não fragmentar
        if (!result.isEmpty() && key(result.get(0)).equals(key(fromFile))) {
            return result;
        }
        // Sem tag, o nome completo do arquivo também é um candidato válido
        if (result.isEmpty()) {
            add(result, fromFile);
        }
        for (String part : SEPARATORS.split(fromFile)) {
            add(result, part);
        }
        return result;
    }

    private static void add(List<String> list, String name) {
        if (name == null || list.size() >= MAX) return;
        String trimmed = name.trim();
        if (trimmed.isEmpty()) return;
        String key = key(trimmed);
        for (String existing : list) {
            if (key(existing).equals(key)) return;
        }
        list.add(trimmed);
    }

    private static String key(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }
}
