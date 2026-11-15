package fr.botc.notes;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Écran transparent ouvert tant que la touche « libérer la souris » est maintenue :
 * la souris apparaît, la caméra ne bouge plus, le jeu continue.
 * Clic dans la note = mode écriture (reste ouvert même Alt relâché, jusqu'à Échap ou clic à côté).
 * Barre de titre = déplacer, coin bas-droit = redimensionner.
 */
public class NoteScreen extends Screen {
    private static final int HANDLE_SIZE = 7;

    private EditBoxWidget editor;
    private boolean dragging;
    private boolean resizing;
    private double grabX, grabY;
    private int startW, startH;

    public NoteScreen() {
        super(Text.translatable("botc-notes.title"));
    }

    private NoteData data() {
        return NoteData.get();
    }

    @Override
    protected void init() {
        NoteData d = data();
        d.clamp(width, height);
        boolean wasFocused = editor != null && editor.isFocused();
        editor = new EditBoxWidget(textRenderer, d.x, d.y + NoteData.TITLE_H, d.w, d.h - NoteData.TITLE_H,
                NoteRenderer.placeholder(), Text.translatable("botc-notes.title"));
        editor.setMaxLength(20000);
        editor.setText(d.text);
        editor.setChangeListener(s -> d.text = s);
        addDrawableChild(editor);
        if (wasFocused) setFocused(editor);
    }

    /** Recrée la zone de texte à la nouvelle taille/position en gardant le focus. */
    private void relayout() {
        boolean focused = editor.isFocused();
        remove(editor);
        NoteData d = data();
        EditBoxWidget e = new EditBoxWidget(textRenderer, d.x, d.y + NoteData.TITLE_H, d.w, d.h - NoteData.TITLE_H,
                NoteRenderer.placeholder(), Text.translatable("botc-notes.title"));
        e.setMaxLength(20000);
        e.setText(d.text);
        e.setChangeListener(s -> d.text = s);
        editor = e;
        addDrawableChild(editor);
        if (focused) setFocused(editor);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // pas de flou ni d'assombrissement : on continue de voir le jeu
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        NoteData d = data();
        NoteRenderer.drawFrame(ctx, textRenderer, d);
        super.render(ctx, mouseX, mouseY, delta);
        NoteRenderer.drawHandle(ctx, d);
    }

    private boolean inTitle(double mx, double my) {
        NoteData d = data();
        return mx >= d.x && mx < d.x + d.w && my >= d.y && my < d.y + NoteData.TITLE_H;
    }

    private boolean inHandle(double mx, double my) {
        NoteData d = data();
        return mx >= d.x + d.w - HANDLE_SIZE && mx < d.x + d.w && my >= d.y + d.h - HANDLE_SIZE && my < d.y + d.h;
    }

    private boolean inNote(double mx, double my) {
        NoteData d = data();
        return mx >= d.x && mx < d.x + d.w && my >= d.y && my < d.y + d.h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0 && inHandle(mx, my)) {
            resizing = true;
            grabX = mx;
            grabY = my;
            startW = data().w;
            startH = data().h;
            return true;
        }
        if (button == 0 && inTitle(mx, my)) {
            dragging = true;
            grabX = mx - data().x;
            grabY = my - data().y;
            return true;
        }
        if (inNote(mx, my)) {
            return super.mouseClicked(mx, my, button);
        }
        // clic à côté : on arrête d'écrire
        setFocused(null);
        editor.setFocused(false);
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        NoteData d = data();
        if (dragging) {
            d.x = (int) Math.round(mx - grabX);
            d.y = (int) Math.round(my - grabY);
            d.clamp(width, height);
            editor.setX(d.x);
            editor.setY(d.y + NoteData.TITLE_H);
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

    @Override
    public void tick() {
        super.tick();
        boolean writing = editor != null && editor.isFocused();
        if (!writing && !dragging && !resizing && !cursorKeyHeld()) {
            close();
        }
    }

    static boolean cursorKeyHeld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(BotcNotes.cursorKey);
        if (key == null || key.getCode() < 0) return false;
        long window = mc.getWindow().getHandle();
        if (key.getCategory() == InputUtil.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getCode()) == GLFW.GLFW_PRESS;
        }
        if (key.getCategory() == InputUtil.Type.KEYSYM) {
            return InputUtil.isKeyPressed(window, key.getCode());
        }
        return false;
    }

    @Override
    public void removed() {
        data().save();
        super.removed();
    }
}
