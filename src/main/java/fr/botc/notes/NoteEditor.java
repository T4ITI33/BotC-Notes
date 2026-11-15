package fr.botc.notes;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.text.Text;

/** Zone de texte sans fond ni cadre : on voit uniquement le fond translucide de la note. */
public class NoteEditor extends EditBoxWidget {
    public NoteEditor(TextRenderer tr, int x, int y, int w, int h, Text placeholder, Text message) {
        super(tr, x, y, w, h, placeholder, message);
    }

    // Volontairement sans @Override : la signature exacte varie selon les mappings.
    protected void drawBox(DrawContext context, int x, int y, int width, int height) {
    }

    protected void drawBox(DrawContext context) {
    }
}