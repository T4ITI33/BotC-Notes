package fr.botc.notes;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** Réglages du mod : mode de la touche qui libère la souris. */
public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Text.translatable("botc-notes.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        NoteData d = NoteData.get();
        addDrawableChild(CyclingButtonWidget.<Boolean>builder(v -> Text.translatable(
                        v ? "botc-notes.config.cursor_mode.hold" : "botc-notes.config.cursor_mode.press"))
                .values(false, true)
                .initially(d.cursorHold)
                .tooltip(v -> net.minecraft.client.gui.tooltip.Tooltip.of(Text.translatable(
                        v ? "botc-notes.config.cursor_mode.hold.tip" : "botc-notes.config.cursor_mode.press.tip")))
                .build(width / 2 - 125, height / 2 - 24, 250, 20,
                        Text.translatable("botc-notes.config.cursor_mode",
                                BotcNotes.cursorKey.getBoundKeyLocalizedText()),
                        (btn, v) -> {
                            d.cursorHold = v;
                            d.save();
                        }));
        addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> close())
                .dimensions(width / 2 - 100, height / 2 + 10, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, height / 2 - 50, 0xFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
