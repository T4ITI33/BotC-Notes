package fr.botc.notes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.List;

/** Dessin de la note : cadre + barre de titre, et le texte quand on n'est pas en train d'écrire. */
public final class NoteRenderer {
    public static final int BG = 0xA0101010;
    public static final int TITLE_BG = 0xE05A1A1A;
    public static final int TITLE_FG = 0xFFE8C9A0;
    public static final int TEXT_FG = 0xFFE0E0E0;
    public static final int HANDLE = 0xFFB0B0B0;
    public static final int PLACEHOLDER_FG = 0xFF8A8A8A;

    private NoteRenderer() {}

    public static void drawFrame(DrawContext ctx, TextRenderer tr, NoteData d) {
        ctx.fill(d.x, d.y, d.x + d.w, d.y + d.h, BG);
        ctx.fill(d.x, d.y, d.x + d.w, d.y + NoteData.TITLE_H, TITLE_BG);
        ctx.drawText(tr, Text.translatable("botc-notes.title"), d.x + 4, d.y + 2, TITLE_FG, false);
    }

    public static void drawHandle(DrawContext ctx, NoteData d) {
        int x2 = d.x + d.w, y2 = d.y + d.h;
        for (int i = 0; i < 6; i++) {
            ctx.fill(x2 - 1 - i, y2 - 6 + i, x2 - 1 - i + 1 + i, y2 - 5 + i, HANDLE);
        }
    }

    /** Texte d'aide affiché quand la note est vide, avec les touches réellement attribuées. */
    public static Text placeholder() {
        return Text.translatable("botc-notes.placeholder",
                BotcNotes.toggleKey.getBoundKeyLocalizedText(),
                BotcNotes.cursorKey.getBoundKeyLocalizedText());
    }

    /** Affichage permanent pendant le jeu (souris non libérée). */
    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NoteData d = NoteData.get();
        if (!d.visible || mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen instanceof NoteScreen) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        d.clamp(ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        TextRenderer tr = mc.textRenderer;
        drawFrame(ctx, tr, d);

        int top = d.y + NoteData.TITLE_H + 4;
        int bottom = d.y + d.h - 2;
        boolean empty = d.text.isEmpty();
        StringVisitable content = empty ? placeholder() : StringVisitable.plain(d.text);
        int color = empty ? PLACEHOLDER_FG : TEXT_FG;
        List<OrderedText> lines = tr.wrapLines(content, d.w - 8);
        for (OrderedText line : lines) {
            if (top + tr.fontHeight > bottom) break;
            ctx.drawText(tr, line, d.x + 4, top, color, false);
            top += tr.fontHeight;
        }
    }
}
