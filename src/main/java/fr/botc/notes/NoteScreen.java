package fr.botc.notes;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Écran transparent d'édition : le jeu continue, la souris est libre.
 * Se ferme avec Échap, la touche d'ouverture (hors écriture) ou un clic à côté de la note.
 */
public class NoteScreen extends Screen {
    private static final int HANDLE_SIZE = 12;

    private NoteEditor editor;
    private boolean dragging;
    private boolean resizing;
    private double grabX, grabY;
    private int startW, startH;

    private boolean renaming;
    private final StringBuilder renameBuf = new StringBuilder();
    private int confirmDelete = -1;
    private long lastClickTime;
    private int lastClickTab = -1;

    public NoteScreen() {
        super(Text.translatable("botc-notes.title"));
    }

    private NoteData data() {
        return NoteData.get();
    }

    // ---------- Éditeur ----------

    private NoteEditor createEditor() {
        NoteData d = data();
        NoteEditor e = new NoteEditor(textRenderer,
                d.x + NoteData.LEFT, d.y + NoteData.TAB_H + 2,
                d.w - NoteData.LEFT - NoteData.BORDER, d.h - NoteData.TAB_H - 2 - NoteData.BORDER,
                NoteRenderer.placeholder(), Text.translatable("botc-notes.title"));
        e.setText(d.current());
        e.setChangeListener(s -> d.setCurrent(s));
        return e;
    }

    @Override
    protected void init() {
        data().clamp(width, height);
        boolean wasFocused = editor != null && editor.isFocused();
        editor = createEditor();
        addDrawableChild(editor);
        if (wasFocused) setFocused(editor);
    }

    private void relayout() {
        boolean focused = editor.isFocused();
        remove(editor);
        editor = createEditor();
        addDrawableChild(editor);
        if (focused) setFocused(editor);
    }

    /** Mode « maintenir » : on ferme dès que la touche est relâchée, sauf pendant l'écriture,
     *  le renommage ou un déplacement/redimensionnement en cours. */
    @Override
    public void tick() {
        super.tick();
        if (!data().cursorHold || renaming || dragging || resizing) return;
        if (editor != null && editor.isFocused()) return;
        if (!BotcNotes.isCursorKeyDown(client)) close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // pas de flou ni d'assombrissement
    }

    // ---------- Rendu ----------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NoteData d = data();
        NoteRenderer.drawFrame(ctx, textRenderer, d);
        super.render(ctx, mouseX, mouseY, delta);
        NoteRenderer.drawHandle(ctx, d);
        NoteRenderer.drawTabs(ctx, textRenderer, d, true, mouseX, mouseY, confirmDelete);

        if (renaming) {
            int tx = NoteRenderer.tabX(textRenderer, d, d.page);
            int tw = Math.max(NoteRenderer.tabWidths(textRenderer, d)[d.page], 80);
            ctx.fill(tx, d.y + 1, tx + tw, d.y + NoteData.TAB_H, 0xFF202020);
            ctx.fill(tx, d.y + NoteData.TAB_H - 1, tx + tw, d.y + NoteData.TAB_H, NoteRenderer.ACCENT);
            String s = renameBuf + (System.currentTimeMillis() / 500 % 2 == 0 ? "_" : "");
            ctx.drawText(textRenderer, s, tx + 3, d.y + 3, 0xFFFFFFFF, false);
        }
    }

    // ---------- Zones ----------

    private boolean inHandle(double mx, double my) {
        NoteData d = data();
        return mx >= d.x + d.w - HANDLE_SIZE && mx < d.x + d.w && my >= d.y + d.h - HANDLE_SIZE && my < d.y + d.h;
    }

    private boolean inNote(double mx, double my) {
        NoteData d = data();
        return mx >= d.x && mx < d.x + d.w && my >= d.y && my < d.y + d.h;
    }

    /** Zone de déplacement : barre d'onglets (partie libre) + bords. */
    private boolean inBorder(double mx, double my) {
        NoteData d = data();
        if (!inNote(mx, my)) return false;
        int b = NoteData.BORDER;
        return my < d.y + NoteData.TAB_H || mx < d.x + NoteData.LEFT_GRAB || mx >= d.x + d.w - b || my >= d.y + d.h - b;
    }

    // ---------- Onglets ----------

    private void switchTo(int i) {
        NoteData d = data();
        if (i < 0 || i >= d.tabs.size() || i == d.page) return;
        d.page = i;
        relayout();
        d.save();
    }

    private void startRename() {
        renaming = true;
        renameBuf.setLength(0);
        renameBuf.append(data().currentTab().name);
        setFocused(null);
        editor.setFocused(false);
    }

    private void commitRename() {
        String s = renameBuf.toString().trim();
        data().currentTab().name = s.isEmpty() ? "Untitled" : s;
        renaming = false;
        data().save();
    }

    private void deleteTab(int i) {
        NoteData d = data();
        if (d.tabs.size() <= 1) return;
        d.tabs.remove(i);
        if (d.page > i || d.page >= d.tabs.size()) d.page = Math.max(0, d.page - 1);
        relayout();
        d.save();
    }

    // ---------- Souris ----------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        NoteData d = data();
        if (renaming) {
            commitRename();
            return true;
        }

        int tab = NoteRenderer.tabAt(textRenderer, d, mx, my);
        if (tab == NoteRenderer.PLUS) {
            confirmDelete = -1;
            if (button == 0 && d.tabs.size() < NoteData.MAX_TABS) {
                d.tabs.add(new NoteData.Tab());
                d.page = d.tabs.size() - 1;
                relayout();
                startRename();
            }
            return true;
        }
        if (tab >= 0) {
            if (button == 0) {
                if (NoteRenderer.inClose(textRenderer, d, tab, mx)) {
                    if (confirmDelete == tab || d.tabs.get(tab).text.isEmpty()) {
                        confirmDelete = -1;
                        deleteTab(tab);
                    } else {
                        confirmDelete = tab;
                    }
                    return true;
                }
                confirmDelete = -1;
                long now = System.currentTimeMillis();
                boolean dbl = tab == lastClickTab && now - lastClickTime < 350;
                lastClickTime = now;
                lastClickTab = tab;
                switchTo(tab);
                if (dbl) startRename();
            } else if (button == 1) {
                confirmDelete = -1;
                switchTo(tab);
                startRename();
            }
            return true;
        }
        confirmDelete = -1;

        if (button == 0 && inHandle(mx, my)) {
            resizing = true;
            grabX = mx;
            grabY = my;
            startW = d.w;
            startH = d.h;
            return true;
        }
        if (button == 0 && inBorder(mx, my)) {
            dragging = true;
            grabX = mx - d.x;
            grabY = my - d.y;
            return true;
        }
        if (inNote(mx, my)) {
            return super.mouseClicked(mx, my, button);
        }
        close();
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        NoteData d = data();
        if (dragging) {
            d.x = (int) Math.round(mx - grabX);
            d.y = (int) Math.round(my - grabY);
            d.clamp(width, height);
            editor.setX(d.x + NoteData.LEFT);
            editor.setY(d.y + NoteData.TAB_H + 2);
            return true;
        }
        if (resizing) {
            d.w = (int) Math.round(startW + (mx - grabX));
            d.h = (int) Math.round(startH + (my - grabY));
            d.w = Math.max(NoteData.MIN_W, Math.min(d.w, width - d.x));
            d.h = Math.max(NoteData.MIN_H, Math.min(d.h, height - d.y));
            relayout();
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging || resizing) {
            dragging = false;
            resizing = false;
            data().save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    // ---------- Clavier ----------

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (renaming) {
            if (chr >= 32 && chr != 127 && renameBuf.length() < NoteData.MAX_NAME) renameBuf.append(chr);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (renaming) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                commitRename();
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                renaming = false;
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && renameBuf.length() > 0) {
                renameBuf.setLength(renameBuf.length() - 1);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (BotcNotes.cursorKey.matchesKey(keyCode, scanCode) && !data().cursorHold
                && !(editor != null && editor.isFocused())) {
            close();
            return true;
        }

        int n = Math.min(9, data().tabs.size());
        if (hasControlDown() && keyCode >= GLFW.GLFW_KEY_1 && keyCode < GLFW.GLFW_KEY_1 + n) {
            switchTo(keyCode - GLFW.GLFW_KEY_1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        data().save();
        super.removed();
    }
}