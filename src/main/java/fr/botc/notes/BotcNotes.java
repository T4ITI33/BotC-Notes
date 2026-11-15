package fr.botc.notes;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.WorldSavePath;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotcNotes implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("botc-notes");

    public static KeyBinding toggleKey;
    public static KeyBinding cursorKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.botc-notes.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, "category.botc-notes"));
        cursorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.botc-notes.cursor", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, "category.botc-notes"));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                NoteData.get().enterWorld(worldKey(client)));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                NoteData.get().leaveWorld());

        ClientTickEvents.END_CLIENT_TICK.register(BotcNotes::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> NoteRenderer.renderHud(ctx));
    }

    private static void tick(MinecraftClient mc) {
        NoteData d = NoteData.get();
        while (toggleKey.wasPressed()) {
            d.visible = !d.visible;
            d.save();
        }
        while (cursorKey.wasPressed()) {
            if (mc.currentScreen == null && mc.player != null && d.visible) {
                mc.setScreen(new NoteScreen());
            }
        }
    }

    private static String worldKey(MinecraftClient mc) {
        String raw;
        IntegratedServer server = mc.getServer();
        if (server != null) {
            raw = "solo_" + server.getSavePath(WorldSavePath.ROOT).normalize().getFileName();
        } else {
            ServerInfo info = mc.getCurrentServerEntry();
            raw = info != null ? "mp_" + info.address : "other";
        }
        return raw.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}