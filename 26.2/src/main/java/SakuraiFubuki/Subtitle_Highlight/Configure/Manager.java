package SakuraiFubuki.Subtitle_Highlight.Configure;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class Manager {
    public static final File file = new File(FabricLoader.getInstance().getConfigDir().toFile().getPath(), "subtitle_highlight.json");
    public static Settings settings;
    public static Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();

    /**
     * 兜底：Gson 反序列化不会执行字段初始化，旧版/手改过、缺字段的 config 里
     * colorSettings、block、entity.mob.player 等都可能是 null，
     * 这里统一补齐为内置默认值，避免渲染线程里 NPE，也避免「颜色全白/全黑」这类怪现象。
     */
    private static void normalize() {
        if (settings == null) {
            settings = new Settings();
        }
        Settings defaults = new Settings();
        if (settings.colorSettings == null) {
            settings.colorSettings = defaults.colorSettings;
        }
        if (settings.colorSettings.block == null) {
            settings.colorSettings.block = defaults.colorSettings.block;
        }
        if (settings.colorSettings.item == null) {
            settings.colorSettings.item = defaults.colorSettings.item;
        }
        if (settings.colorSettings.entity == null) {
            settings.colorSettings.entity = defaults.colorSettings.entity;
        }
        if (settings.colorSettings.entity.mob == null) {
            settings.colorSettings.entity.mob = defaults.colorSettings.entity.mob;
        }
        if (settings.colorSettings.entity.mob.player == null) {
            settings.colorSettings.entity.mob.player = defaults.colorSettings.entity.mob.player;
        }
        if (settings.customList == null) {
            settings.customList = new ArrayList<>();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, false)) {
            writer.write(gson.toJson(settings));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load() {
        if (!file.exists() || !file.isFile()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            settings = new Settings();
            save();
        } else {
            try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                char[] content = new char[(int) file.length()];
                reader.read(content);
                try {
                    settings = gson.fromJson(new String(content).trim(), Settings.class);
                    if (settings == null) {
                        settings = new Settings();
                        save();
                    }
                } catch (JsonSyntaxException e) {
                    settings = new Settings();
                    save();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        normalize();
    }

    public static void exportConfig(File exportFile) {
        try (FileWriter writer = new FileWriter(exportFile, StandardCharsets.UTF_8, false)) {
            writer.write(gson.toJson(settings));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void importConfig(File importFile) {
        if (!importFile.exists() || !importFile.isFile()) {
            throw new IllegalArgumentException("Import file does not exist or is not a file");
        }
        try (FileReader reader = new FileReader(importFile, StandardCharsets.UTF_8)) {
            char[] content = new char[(int) importFile.length()];
            reader.read(content);
            try {
                settings = gson.fromJson(new String(content).trim(), Settings.class);
                if (settings == null) {
                    throw new IllegalArgumentException("Invalid configuration file format");
                }
                normalize();
                save();
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("Invalid configuration file format", e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
