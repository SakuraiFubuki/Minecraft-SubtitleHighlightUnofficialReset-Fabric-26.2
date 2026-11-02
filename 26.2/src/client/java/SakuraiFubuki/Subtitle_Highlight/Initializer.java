package SakuraiFubuki.Subtitle_Highlight;

import SakuraiFubuki.Subtitle_Highlight.Command.CommandManager;
import SakuraiFubuki.Subtitle_Highlight.Configure.Manager;
import SakuraiFubuki.Subtitle_Highlight.Data.SubtitleTypeLoader;
import SakuraiFubuki.Subtitle_Highlight.Keybind.KeybindManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

public class Initializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Manager.load();

        ClientCommandRegistrationCallback.EVENT.register(CommandManager::registerCommands);

        // 26.2：ResourceManagerHelper / ResourceType 已被资源 API v1 取代
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(SubtitleTypeLoader.ID, new SubtitleTypeLoader());

        KeybindManager.registerKeybinds();
    }
}
