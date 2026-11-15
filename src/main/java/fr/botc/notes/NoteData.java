package fr.botc.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Contenu et position du bloc-notes, enregistrés dans config/botc-notes.json. */
public class NoteData {
    public static final int TITLE_H = 11;
    public static final int MIN_W = 70;
    public static final int MIN_H = 40;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("botc-notes.json");
    private static NoteData instance;

    public boolean visible = true;
    public int x = 8;
    public int y = 40;
    public int w = 170;
    public int h = 110;
    public String text = "";

    public static NoteData get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static NoteData load() {
        try {
            if (Files.exists(FILE)) {
                NoteData d = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), NoteData.class);
                if (d != null) {
                    if (d.text == null) d.text = "";
                    return d;
                }
            }
        } catch (Exception e) {
            BotcNotes.LOGGER.warn("Lecture de botc-notes.json impossible, bloc-notes vide", e);
        }
        return new NoteData();
    }

    public void save() {
        try {
            Files.writeString(FILE, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException e) {
            BotcNotes.LOGGER.warn("Enregistrement de botc-notes.json impossible", e);
        }
    }

    /** Garde la note entièrement visible quelle que soit la taille de la fenêtre. */
    public void clamp(int screenW, int screenH) {
        w = Math.max(MIN_W, Math.min(w, screenW));
        h = Math.max(MIN_H, Math.min(h, screenH));
        x = Math.max(0, Math.min(x, screenW - w));
        y = Math.max(0, Math.min(y, screenH - h));
    }
}
