package fr.botc.notes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.List;

public final class NoteRenderer {
    public static final int BG = 0xA0101010;
    public static final int TABBAR_BG = 0xC0000000;
    public static final int TEXT_FG = 0xFFE0E0E0;
    public static final int HANDLE = 0xFFB0B0B0;
    public static final int PLACEHOLDER_FG = 0xFF8A8A8A;
    public static final int ACCENT = 0xFFE8C9A0;
    public static final int OUTLINE = 0x40FFFFFF;
    /** Marge intérieure de la zone de texte (EditBoxWidget), reprise par le HUD pour ne rien décaler. */
    public static final int TEXT_PAD = 4;

    public static final int NONE = -1;
    public static final int PLUS = -2;
    public static final int PLUS_W = 14;

    private NoteRenderer() {}

    public static void drawFrame(DrawContext ctx, TextRenderer tr, NoteData d) {
        ctx.fill(d.x, d.y, d.x + d.w, d.y + d.h, BG);
        ctx.fill(d.x, d.y, d.x + d.w, d.y + NoteData.TAB_H, TABBAR_BG);
        // contour toujours affiché (avant : seulement avec Alt, ce qui décalait l'affichage)
        int c = OUTLINE;
        ctx.fill(d.x, d.y, d.x + d.w, d.y + 1, c);
        ctx.fill(d.x, d.y + d.h - 1, d.x + d.w, d.y + d.h, c);
        ctx.fill(d.x, d.y, d.x + 1, d.y + d.h, c);
        ctx.fill(d.x + d.w - 1, d.y, d.x + d.w, d.y + d.h, c);
    }

    public static void drawHandle(DrawContext ctx, NoteData d) {
        int x2 = d.x + d.w, y2 = d.y + d.h;
        for (int i = 0; i < 10; i++) {
            ctx.fill(x2 - 1 - i, y2 - 10 + i, x2 - 1, y2 - 9 + i, HANDLE);
        }
    }

    // ---------- Onglets ----------

    public static int[] tabWidths(TextRenderer tr, NoteData d) {
        int n = d.tabs.size();
        int[] w = new int[n];
        int total = 0;
        for (int i = 0; i < n; i++) {
            w[i] = Math.max(34, Math.min(90, tr.getWidth(d.tabs.get(i).name) + 20));
            total += w[i];
        }
        int avail = d.w - PLUS_W;
        if (total > avail) {
            for (int i = 0; i < n; i++) w[i] = Math.max(14, w[i] * avail / total);
        }
        return w;
    }

    public static int tabX(TextRenderer tr, NoteData d, int tab) {
        int[] w = tabWidths(tr, d);
        int x = d.x;
        for (int i = 0; i < tab; i++) x += w[i];
        return x;
    }

    /** Index de l'onglet sous la souris, PLUS pour le bouton +, NONE sinon. */
    public static int tabAt(TextRenderer tr, NoteData d, double mx, double my) {
        if (my < d.y || my >= d.y + NoteData.TAB_H || mx < d.x) return NONE;
        int[] w = tabWidths(tr, d);
        int x = d.x;
        for (int i = 0; i < w.length; i++) {
            if (mx < x + w[i]) return i;
            x += w[i];
        }
        return mx < x + PLUS_W ? PLUS : NONE;
    }

    /** Clic sur le petit x de l'onglet actif. */
    public static boolean inClose(TextRenderer tr, NoteData d, int tab, double mx) {
        if (tab != d.page || d.tabs.size() <= 1) return false;
        int x = tabX(tr, d, tab) + tabWidths(tr, d)[tab];
        return mx >= x - 12;
    }

    public static void drawTabs(DrawContext ctx, TextRenderer tr, NoteData d,
                                boolean editing, int mx, int my, int confirmTab) {
        int[] w = tabWidths(tr, d);
        int hover = editing ? tabAt(tr, d, mx, my) : NONE;
        int x = d.x;
        for (int i = 0; i < w.length; i++) {
            boolean active = i == d.page;
            int top = d.y + 1;
            int bottom = d.y + NoteData.TAB_H;
            int bg = active ? 0x70FFFFFF : (i == hover ? 0x40FFFFFF : 0x20FFFFFF);
            ctx.fill(x, top, x + w[i] - 1, bottom, bg);
            if (active) ctx.fill(x, bottom - 2, x + w[i] - 1, bottom, ACCENT);

            boolean showClose = editing && active && d.tabs.size() > 1;
            int nameW = w[i] - 6 - (showClose ? 10 : 0);
            String name = tr.trimToWidth(d.tabs.get(i).name, Math.max(4, nameW));
            ctx.drawText(tr, name, x + 3, d.y + 3, active ? 0xFFFFFFFF : 0xFFB8B8B8, false);

            if (showClose) {
                boolean confirm = i == confirmTab;
                ctx.drawText(tr, confirm ? "!" : "x", x + w[i] - 9, d.y + 3,
                        confirm ? 0xFFFF5555 : 0xFFDDDDDD, false);
            }
            x += w[i];
        }
        if (editing && d.tabs.size() < NoteData.MAX_TABS) {
            boolean h = hover == PLUS;
            ctx.fill(x + 1, d.y + 1, x + PLUS_W - 1, d.y + NoteData.TAB_H, h ? 0x60FFFFFF : 0x30FFFFFF);
            ctx.drawText(tr, "+", x + (PLUS_W - tr.getWidth("+")) / 2, d.y + 3, 0xFFFFFFFF, false);
        }
    }

    public static Text placeholder() {
        return Text.translatable("botc-notes.placeholder",
                BotcNotes.toggleKey.getBoundKeyLocalizedText(),
                BotcNotes.cursorKey.getBoundKeyLocalizedText());
    }

    /** Affichage permanent pendant le jeu. */
    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        NoteData d = NoteData.get();
        if (!d.visible || mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen instanceof NoteScreen) return;
        if (mc.getDebugHud().shouldShowDebugHud()) return;

        d.clamp(ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        TextRenderer tr = mc.textRenderer;
        drawFrame(ctx, tr, d);
        drawTabs(ctx, tr, d, false, 0, 0, -1);

        String text = d.current();
        boolean empty = text.isEmpty();
        StringVisitable content = empty ? placeholder() : StringVisitable.plain(text);
        int color = empty ? PLACEHOLDER_FG : TEXT_FG;
        List<OrderedText> lines = tr.wrapLines(content, d.w - NoteData.LEFT - NoteData.BORDER - 2 * TEXT_PAD);

        int top = d.y + NoteData.TAB_H + 2 + TEXT_PAD;
        int bottom = d.y + d.h - NoteData.BORDER - TEXT_PAD;
        int maxLines = Math.max(1, (bottom - top) / tr.fontHeight);
        int start = Math.max(0, lines.size() - maxLines);
        for (int i = start; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), d.x + NoteData.LEFT + TEXT_PAD, top, color, true);
            top += tr.fontHeight;
        }
    }
}