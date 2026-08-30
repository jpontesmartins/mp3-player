package com.ovelha.fy.player.playlist.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Agregado de domínio para uma playlist virtual. Cada música é referenciada
 * pelo caminho absoluto do arquivo físico, na ordem da playlist.
 */
public final class Playlist {

    private final String name;
    private final List<String> songPaths;

    /**
     * Cria uma nova playlist.
     *
     * @param name     nome da playlist; {@code null} resulta em string vazia.
     * @param songPaths lista de caminhos absolutos das músicas; {@code null} resulta em lista vazia.
     */
    public Playlist(String name, List<String> songPaths) {
        this.name = name == null ? "" : name.trim();
        this.songPaths = songPaths == null ? List.of() : List.copyOf(songPaths);
    }

    /**
     * Retorna o nome da playlist.
     *
     * @return nome da playlist.
     */
    public String getName() {
        return name;
    }

    /**
     * Retorna os caminhos das músicas da playlist em ordem.
     *
     * @return lista imutável de caminhos absolutos.
     */
    public List<String> getSongPaths() {
        return Collections.unmodifiableList(songPaths);
    }

    /**
     * Retorna uma nova playlist com a música informada adicionada ao final,
     * caso ainda não esteja presente.
     *
     * @param path caminho absoluto do arquivo da música.
     * @return nova instância com a música adicionada, ou {@code this} se já existir.
     */
    public Playlist addSong(String path) {
        Objects.requireNonNull(path);
        if (songPaths.contains(path)) {
            return this;
        }
        List<String> next = new ArrayList<>(songPaths);
        next.add(path);
        return new Playlist(name, next);
    }

    /**
     * Retorna uma nova playlist sem a música informada.
     *
     * @param path caminho absoluto do arquivo da música.
     * @return nova instância sem a música, ou {@code this} se ela não estiver presente.
     */
    public Playlist removeSong(String path) {
        List<String> next = new ArrayList<>(songPaths);
        next.remove(path);
        return new Playlist(name, next);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist other)) return false;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}