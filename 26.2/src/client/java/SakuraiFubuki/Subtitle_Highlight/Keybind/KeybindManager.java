package SakuraiFubuki.Subtitle_Highlight.Keybind;

import SakuraiFubuki.Subtitle_Highlight.Configure.Manager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeybindManager {
    private static KeyMapping toggleSubtitles;
    private static KeyMapping increaseScale;
    private static KeyMapping decreaseScale;
    private static KeyMapping reloadConfig;
    private static KeyMapping saveConfig;
    private static KeyMapping toggleIkunEasterEgg;
    private static KeyMapping increaseOpacity;
    private static KeyMapping decreaseOpacity;

    public static void registerKeybinds() {
        // 26.2：KeyBinding → KeyMapping，KeyBinding.Category → KeyMapping.Category（record），
        // 注册器 KeyBindingHelper → KeyMappingHelper.registerKeyMapping
        KeyMapping.Category category = new KeyMapping.Category(Identifier.fromNamespaceAndPath("subtitle_highlight", "keybinds"));

        toggleSubtitles = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.toggle_subtitles",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        increaseScale = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.increase_scale",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        decreaseScale = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.decrease_scale",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        reloadConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.reload_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        saveConfig = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.save_config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        toggleIkunEasterEgg = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.toggle_ikun",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        increaseOpacity = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.increase_opacity",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        decreaseOpacity = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.subtitle_highlight.decrease_opacity",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::handleKeybinds);
    }

    private static void handleKeybinds(Minecraft client) {
        if (client.player == null) return;

        while (toggleSubtitles.consumeClick()) {
            boolean newValue = !client.options.showSubtitles().get();
            client.options.showSubtitles().set(newValue);
            client.options.save();
            client.player.sendOverlayMessage(Component.translatable(newValue ? "subtitle_highlight.keybind.toggle_subtitles.on" : "subtitle_highlight.keybind.toggle_subtitles.off"));
        }

        while (increaseScale.consumeClick()) {
            Manager.settings.scale = Math.min(Manager.settings.scale + 0.1f, 3.0f);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.scale.changed", String.format("%.1f", Manager.settings.scale)));
        }

        while (decreaseScale.consumeClick()) {
            Manager.settings.scale = Math.max(Manager.settings.scale - 0.1f, 0.5f);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.scale.changed", String.format("%.1f", Manager.settings.scale)));
        }

        while (reloadConfig.consumeClick()) {
            Manager.load();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.reload.success"));
        }

        while (saveConfig.consumeClick()) {
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.save.success"));
        }

        while (toggleIkunEasterEgg.consumeClick()) {
            Manager.settings.ikunEasterEgg = !Manager.settings.ikunEasterEgg;
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable(Manager.settings.ikunEasterEgg ? "subtitle_highlight.keybind.ikun.on" : "subtitle_highlight.keybind.ikun.off"));
        }

        while (increaseOpacity.consumeClick()) {
            int alpha = (Manager.settings.backgroundColor >>> 24) & 0xFF;
            alpha = Math.min(alpha + 17, 255);
            Manager.settings.backgroundColor = (alpha << 24) | (Manager.settings.backgroundColor & 0x00FFFFFF);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.opacity.changed", String.format("%.0f%%", (alpha / 255.0) * 100)));
        }

        while (decreaseOpacity.consumeClick()) {
            int alpha = (Manager.settings.backgroundColor >>> 24) & 0xFF;
            alpha = Math.max(alpha - 17, 0);
            Manager.settings.backgroundColor = (alpha << 24) | (Manager.settings.backgroundColor & 0x00FFFFFF);
            Manager.save();
            client.player.sendOverlayMessage(Component.translatable("subtitle_highlight.keybind.opacity.changed", String.format("%.0f%%", (alpha / 255.0) * 100)));
        }
    }
}
