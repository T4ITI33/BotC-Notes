package fr.botc.notes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Contenu et position du bloc-notes, enregistrés dans config/botc-notes.json. */
public class NoteData {
    public static final int TITLE_H = 0;   // plus de barre de titre
    public static final int BORDER = 12;    // zone de saisie pour déplacer la note
    public static final int MIN_W = 90;
    public static final int MIN_H = 50;
    public static final int TAB_H = 14;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("botc-notes.json");
    private static NoteData instance;

    public boolean visible = true;
    public int x = 8;
    public int y = 40;
    public int w = 170;
    public int h = 110;
    public int alpha = 140;   // 40 à 255
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
                    d.normalize();
                    d.legacyPending = true;
                    return d;
                }
            }
        } catch (Exception e) {
            BotcNotes.LOGGER.warn("Lecture de botc-notes.json impossible, bloc-notes vide", e);
        }
        NoteData n = new NoteData();
        n.normalize();
        return n;
    }

    public void save() {
        try {
            JsonObject o = GSON.toJsonTree(this).getAsJsonObject();
            o.remove("tabs");
            o.remove("page");
            o.remove("pages");
            o.remove("text");
            Files.writeString(FILE, GSON.toJson(o), StandardCharsets.UTF_8);
        } catch (IOException e) {
            BotcNotes.LOGGER.warn("Enregistrement de botc-notes.json impossible", e);
        }
        saveWorld();
    }

    /** Garde la note entièrement visible quelle que soit la taille de la fenêtre. */
    public void clamp(int screenW, int screenH) {
        w = Math.max(MIN_W, Math.min(w, screenW));
        h = Math.max(MIN_H, Math.min(h, screenH));
        x = Math.max(0, Math.min(x, screenW - w));
        y = Math.max(0, Math.min(y, screenH - h));
    }

        public static final int MAX_TABS = 12;
    public static final int MAX_NAME = 16;

    public static class Tab {
        public String name = "Untitled";
        public String text = "";
    }

    public List<Tab> tabs = new ArrayList<>();
    public int page = 0;
    public String[] pages;   // ancien format (migration uniquement)

    public Tab currentTab() {
        return tabs.get(page);
    }

    public String current() {
        return currentTab().text;
    }

    public void setCurrent(String s) {
        currentTab().text = s;
    }

    private void normalize() {
        if (tabs == null) tabs = new ArrayList<>();
        if (tabs.isEmpty()) {
            if (pages != null) {
                for (int i = 0; i < pages.length; i++) {
                    if (pages[i] != null && !pages[i].isEmpty()) {
                        Tab t = new Tab();
                        t.name = "Page " + (i + 1);
                        t.text = pages[i];
                        tabs.add(t);
                    }
                }
            }
            if (tabs.isEmpty()) {
                Tab t = new Tab();
                if (text != null) t.text = text;
                tabs.add(t);
            }
        }
        for (Tab t : tabs) {
            if (t.name == null || t.name.isBlank()) t.name = "Untitled";
            if (t.text == null) t.text = "";
        }
        pages = null;
        text = "";
        page = Math.max(0, Math.min(page, tabs.size() - 1));
    }

    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("botc-notes");

    private transient String worldKey;
    private transient boolean legacyPending;   // anciennes notes pas encore attribuées à un monde

    public static class WorldNotes {
        public List<Tab> tabs;
        public int page;
    }

/** Appelé à l'entrée dans un monde : charge les notes de ce monde. */
    public void enterWorld(String key) {
        leaveWorld();
        worldKey = key;
        WorldNotes w = null;
        try {
            Path f = DIR.resolve(key + ".json");
            if (Files.exists(f)) {
                w = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), WorldNotes.class);
            }
        } catch (Exception e) {
            BotcNotes.LOGGER.warn("Lecture des notes du monde impossible", e);
        }
        if (w != null && w.tabs != null && !w.tabs.isEmpty()) {
            tabs = w.tabs;
            page = w.page;
        } else if (!legacyPending) {
            tabs = new ArrayList<>();
            tabs.add(new Tab());
            page = 0;
        }
    // si legacyPending : les anciennes notes sont reprises par le premier monde ouvert
        legacyPending = false;
        normalize();
        saveWorld();
    }

/** Appelé à la sortie d'un monde. */
    public void leaveWorld() {
        if (worldKey != null) saveWorld();
        worldKey = null;
    }

    private void saveWorld() {
        if (worldKey == null) return;
        try {
            Files.createDirectories(DIR);
            WorldNotes w = new WorldNotes();
            w.tabs = tabs;
            w.page = page;
            Files.writeString(DIR.resolve(worldKey + ".json"), GSON.toJson(w), StandardCharsets.UTF_8);
        } catch (IOException e) {
            BotcNotes.LOGGER.warn("Enregistrement des notes du monde impossible", e);
        }
    }

}
