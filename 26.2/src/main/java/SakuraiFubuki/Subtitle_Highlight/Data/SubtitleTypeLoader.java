package SakuraiFubuki.Subtitle_Highlight.Data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import SakuraiFubuki.Subtitle_Highlight.Util.ColorCode;
import net.fabricmc.fabric.api.resource.v1.reloader.SimpleReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据包驱动的字幕颜色表。
 *
 * <p>26.2 的 Fabric 资源 API 换到了 {@code net.fabricmc.fabric.api.resource.v1}：
 * 老的 {@code ResourceManagerHelper} + {@code SimpleSynchronousResourceReloadListener}
 * 已不存在，改为 {@code ResourceLoader.get(PackType).registerReloadListener(...)}
 * 注册 {@link SimpleReloadListener}（prepare / apply 两段式）。</p>
 */
public class SubtitleTypeLoader extends SimpleReloadListener<Map<String, Map<String, ColorCode>>> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("subtitle_highlight", "subtitle_types");
    private static final Gson GSON = new Gson();
    private static Map<String, Map<String, ColorCode>> subtitleTypes = new HashMap<>();

    private static final ConcurrentHashMap<String, ColorCode> colorCache = new ConcurrentHashMap<>();
    private static final String CACHE_KEY_SEPARATOR = ":";

    private static final ColorCode NULL_MARKER = ColorCode.GRAY;

    @Override
    protected Map<String, Map<String, ColorCode>> prepare(PreparableReloadListener.SharedState state) {
        Map<String, Map<String, ColorCode>> result = new HashMap<>();
        ResourceManager manager = state.resourceManager();
        try {
            Optional<Resource> resource = manager.getResource(Identifier.fromNamespaceAndPath("subtitle_highlight", "subtitle_types.json"));
            if (resource.isEmpty()) {
                return buildDefaultSubtitleTypes();
            }
            JsonObject jsonObject;
            try (InputStreamReader reader = new InputStreamReader(resource.get().open())) {
                jsonObject = GSON.fromJson(reader, JsonObject.class);
            }
            if (jsonObject == null || !jsonObject.has("subtitle_types")) {
                return buildDefaultSubtitleTypes();
            }
            loadSubtitleTypes(jsonObject.getAsJsonObject("subtitle_types"), "", result);
            if (result.isEmpty()) {
                return buildDefaultSubtitleTypes();
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return buildDefaultSubtitleTypes();
        }
    }

    @Override
    protected void apply(Map<String, Map<String, ColorCode>> data, PreparableReloadListener.SharedState state) {
        subtitleTypes = data;
        invalidateCache();
    }

    private void loadSubtitleTypes(JsonObject jsonObject, String path, Map<String, Map<String, ColorCode>> result) {
        for (String key : jsonObject.keySet()) {
            String currentPath = path.isEmpty() ? key : path + "." + key;
            if (jsonObject.get(key).isJsonObject()) {
                JsonObject nestedObject = jsonObject.getAsJsonObject(key);
                if (nestedObject.has("color")) {
                    String colorName = nestedObject.get("color").getAsString();
                    ColorCode colorCode = ColorCode.fromName(colorName);
                    if (colorCode != null) {
                        String parentPath = path.isEmpty() ? "" : path;
                        result.computeIfAbsent(parentPath, k -> new HashMap<>()).put(key, colorCode);
                    }
                } else {
                    loadSubtitleTypes(nestedObject, currentPath, result);
                }
            }
        }
    }

    private Map<String, Map<String, ColorCode>> buildDefaultSubtitleTypes() {
        Map<String, Map<String, ColorCode>> result = new HashMap<>();

        Map<String, ColorCode> ambientMap = new HashMap<>();
        ambientMap.put("ambient", ColorCode.DARK_BLUE);
        result.put("", ambientMap);

        Map<String, ColorCode> blockMap = new HashMap<>();
        blockMap.put("generic", ColorCode.GRAY);
        blockMap.put("interact", ColorCode.GREEN);
        blockMap.put("working", ColorCode.YELLOW);
        blockMap.put("dangerous", ColorCode.RED);
        blockMap.put("crop", ColorCode.GREEN);
        blockMap.put("other", ColorCode.GRAY);
        result.put("block", blockMap);

        Map<String, ColorCode> enchantMap = new HashMap<>();
        enchantMap.put("enchant", ColorCode.LIGHT_PURPLE);
        result.put("", enchantMap);

        Map<String, ColorCode> entityMobPlayerMap = new HashMap<>();
        entityMobPlayerMap.put("attack", ColorCode.RED);
        entityMobPlayerMap.put("hurt", ColorCode.RED);
        entityMobPlayerMap.put("other", ColorCode.WHITE);
        result.put("entity.mob.player", entityMobPlayerMap);

        Map<String, ColorCode> entityMobMap = new HashMap<>();
        entityMobMap.put("passive", ColorCode.GREEN);
        entityMobMap.put("neutral", ColorCode.YELLOW);
        entityMobMap.put("hostile", ColorCode.RED);
        entityMobMap.put("boss", ColorCode.DARK_PURPLE);
        result.put("entity.mob", entityMobMap);

        Map<String, ColorCode> entityMap = new HashMap<>();
        entityMap.put("vehicle", ColorCode.GRAY);
        entityMap.put("projectile", ColorCode.AQUA);
        entityMap.put("explosive", ColorCode.RED);
        entityMap.put("decoration", ColorCode.GRAY);
        entityMap.put("other", ColorCode.GRAY);
        result.put("entity", entityMap);

        Map<String, ColorCode> itemMap = new HashMap<>();
        itemMap.put("weapon", ColorCode.RED);
        itemMap.put("armor", ColorCode.GOLD);
        itemMap.put("tool", ColorCode.AQUA);
        itemMap.put("other", ColorCode.GRAY);
        result.put("item", itemMap);

        Map<String, ColorCode> otherMap = new HashMap<>();
        otherMap.put("other", ColorCode.GRAY);
        result.put("", otherMap);

        return result;
    }

    public static ColorCode getColor(String path, String key) {
        String cacheKey = buildCacheKey(path, key);

        ColorCode cached = colorCache.get(cacheKey);
        if (cached != null) {
            return cached == NULL_MARKER ? null : cached;
        }

        Map<String, ColorCode> map = subtitleTypes.get(path);
        ColorCode result = (map != null) ? map.get(key) : null;

        colorCache.put(cacheKey, result != null ? result : NULL_MARKER);

        return result;
    }

    private static String buildCacheKey(String path, String key) {
        return path + CACHE_KEY_SEPARATOR + key;
    }

    public static void invalidateCache() {
        colorCache.clear();
    }
}
