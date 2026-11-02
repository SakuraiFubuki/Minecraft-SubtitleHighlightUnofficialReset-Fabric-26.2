package SakuraiFubuki.Subtitle_Highlight.Mixin;

import SakuraiFubuki.Subtitle_Highlight.Configure.Manager;
import SakuraiFubuki.Subtitle_Highlight.Configure.Settings;
import SakuraiFubuki.Subtitle_Highlight.Data.SubtitleTypeLoader;
import SakuraiFubuki.Subtitle_Highlight.Util.ColorCode;
import SakuraiFubuki.Subtitle_Highlight.Util.SplitKeyArrays;
import SakuraiFubuki.Subtitle_Highlight.api.SubtitleAPI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2：{@code SubtitlesHud$SubtitleEntry} 改名为 {@code SubtitleOverlay$Subtitle}，
 * {@code getText()} 的返回类型从 {@code Text} 变成 {@code Component}。
 *
 * <p>颜色来源优先级（重要）：</p>
 * <ol>
 *   <li>配置里被用户改过的值（{@code Manager.settings.colorSettings.*}）——最高优先级，保证设置界面改完立刻生效；</li>
 *   <li>数据包 {@code data/subtitle_highlight/subtitle_types.json}——仅在配置仍是内置默认值时生效；</li>
 *   <li>内置默认值。</li>
 * </ol>
 * <p>26.2 的资源 API 换成 v1 后，数据包在客户端确实会被加载；若沿用「数据包优先」的老顺序，
 * 内置数据包会盖掉配置界面里的每一项颜色，导致改颜色看起来毫无效果。</p>
 */
@Environment(EnvType.CLIENT)
@Mixin(targets = "net.minecraft.client.gui.components.SubtitleOverlay$Subtitle")
public class SubtitleEntryMixin {
    @Shadow
    @Final
    private Component text;

    private static final Logger PERF_LOGGER = LoggerFactory.getLogger("subtitle_highlight-performance");

    private static long totalTime = 0;
    private static int callCount = 0;
    private static long maxTime = 0;
    private static long minTime = Long.MAX_VALUE;
    private static long lastLogTime = 0;
    private static final long LOG_INTERVAL = 10000;

    /**
     * 决定一项颜色最终用谁。
     *
     * @param path      数据包里的分类路径
     * @param key       数据包里的键
     * @param configured 配置里的当前值（可能为 null，例如旧配置文件缺字段）
     * @param builtin   内置默认值（来自 {@link Settings#BUILTIN_DEFAULTS}）
     */
    private static ColorCode resolve(String path, String key, ColorCode configured, ColorCode builtin) {
        if (configured != null && configured != builtin) {
            // 用户在配置里改过 → 配置优先
            return configured;
        }
        ColorCode fromDataPack = SubtitleTypeLoader.getColor(path, key);
        if (fromDataPack != null) {
            return fromDataPack;
        }
        return configured != null ? configured : builtin;
    }

    @Inject(at = @At("RETURN"), method = "getText()Lnet/minecraft/network/chat/Component;", cancellable = true)
    private void colorizeSubtitle(CallbackInfoReturnable<Component> cir) {
        long startTime = System.nanoTime();

        try {
            // 26.2 的 MutableComponent.withStyle 是原地修改，先 copy() 避免污染原版共享的字幕文本
            MutableComponent subtitleText = this.text.copy().withStyle(ChatFormatting.RESET);
            if (subtitleText.getContents() instanceof TranslatableContents translatable) {
                Settings current = Manager.settings;
                Settings defaults = Settings.BUILTIN_DEFAULTS;

                long customCheckStart = System.nanoTime();
                for (Settings.Custom custom : current.customList) {
                    if (translatable.getKey().equals(custom.translationKey)) {
                        cir.setReturnValue(subtitleText.setStyle(subtitleText.getStyle().withColor(custom.color).withObfuscated(custom.obfuscated).withBold(custom.bold).withStrikethrough(custom.strikethrough).withUnderlined(custom.underline).withItalic(custom.italic)));
                        return;
                    }
                }
                long customCheckTime = System.nanoTime() - customCheckStart;
                if (customCheckTime > 100000) {
                    PERF_LOGGER.warn("Custom list check took {}us for {} entries", customCheckTime / 1000, current.customList.size());
                }

                String[] keyParts = translatable.getKey().split("\\.");
                if (keyParts.length > 1 && keyParts[0].equals("subtitles")) {
                    switch (keyParts[1]) {
                        case "ambient", "weather" -> {
                            applyColor(cir, subtitleText, "", "ambient",
                                    current.colorSettings.ambient, defaults.colorSettings.ambient);
                            return;
                        }
                        case "block" -> {
                            if (keyParts.length < 3) {
                                applyColor(cir, subtitleText, "block", "other",
                                        current.colorSettings.block.other, defaults.colorSettings.block.other);
                                return;
                            }
                            if (keyParts[2].equals("generic")) {
                                applyColor(cir, subtitleText, "block", "generic",
                                        current.colorSettings.block.generic, defaults.colorSettings.block.generic);
                                return;
                            }
                            String blockKey = keyParts[2];
                            String subKey = keyParts.length > 3 ? keyParts[3] : "";

                            if (SplitKeyArrays.interactSet.contains(blockKey)) {
                                if ((blockKey.equals("anvil") && subKey.equals("land")) || (blockKey.equals("tripwire") && subKey.equals("click"))) {
                                    applyColor(cir, subtitleText, "block", "dangerous",
                                            current.colorSettings.block.dangerous, defaults.colorSettings.block.dangerous);
                                } else {
                                    applyColor(cir, subtitleText, "block", "interact",
                                            current.colorSettings.block.interact, defaults.colorSettings.block.interact);
                                }
                                return;
                            }
                            if (SplitKeyArrays.workingSet.contains(blockKey)) {
                                if ((blockKey.equals("beacon") && subKey.equals("power_select")) || (blockKey.equals("beehive") && subKey.equals("shear"))) {
                                    applyColor(cir, subtitleText, "block", "interact",
                                            current.colorSettings.block.interact, defaults.colorSettings.block.interact);
                                } else if (blockKey.equals("pointed_dripstone") && (subKey.startsWith("drip_lava") || subKey.equals("land"))) {
                                    applyColor(cir, subtitleText, "block", "dangerous",
                                            current.colorSettings.block.dangerous, defaults.colorSettings.block.dangerous);
                                } else {
                                    applyColor(cir, subtitleText, "block", "working",
                                            current.colorSettings.block.working, defaults.colorSettings.block.working);
                                }
                                return;
                            }
                            if (SplitKeyArrays.dangerousBlocksSet.contains(blockKey)) {
                                applyColor(cir, subtitleText, "block", "dangerous",
                                        current.colorSettings.block.dangerous, defaults.colorSettings.block.dangerous);
                                return;
                            }
                            if (SplitKeyArrays.cropsSet.contains(blockKey)) {
                                applyColor(cir, subtitleText, "block", "crop",
                                        current.colorSettings.block.crop, defaults.colorSettings.block.crop);
                                return;
                            }
                            applyColor(cir, subtitleText, "block", "other",
                                    current.colorSettings.block.other, defaults.colorSettings.block.other);
                            return;
                        }
                        case "chiseled_bookshelf", "ui" -> {
                            // 这一类没有对应数据包条目，直接用配置
                            ColorCode color = current.colorSettings.block.interact;
                            cir.setReturnValue(subtitleText.withStyle((color != null ? color : defaults.colorSettings.block.interact).getFormatting()));
                            return;
                        }
                        case "enchant", "particle" -> {
                            applyColor(cir, subtitleText, "", "enchant",
                                    current.colorSettings.enchant, defaults.colorSettings.enchant);
                            return;
                        }
                        case "entity" -> {
                            if (keyParts.length < 3) {
                                applyColor(cir, subtitleText, "entity", "other",
                                        current.colorSettings.entity.other, defaults.colorSettings.entity.other);
                                return;
                            }
                            String entityKey = keyParts[2];
                            String subKey = keyParts.length > 3 ? keyParts[3] : "";

                            if (entityKey.equals("generic") || entityKey.equals("player")) {
                                if (subKey.equals("attack")) {
                                    applyColor(cir, subtitleText, "entity.mob.player", "attack",
                                            current.colorSettings.entity.mob.player.attack, defaults.colorSettings.entity.mob.player.attack);
                                } else if (SplitKeyArrays.hurtSet.contains(subKey)) {
                                    applyColor(cir, subtitleText, "entity.mob.player", "hurt",
                                            current.colorSettings.entity.mob.player.hurt, defaults.colorSettings.entity.mob.player.hurt);
                                } else {
                                    applyColor(cir, subtitleText, "entity.mob.player", "other",
                                            current.colorSettings.entity.mob.player.other, defaults.colorSettings.entity.mob.player.other);
                                }
                                return;
                            }

                            if (SplitKeyArrays.friendlyMobsSet.contains(entityKey)) {
                                if (entityKey.equals("chicken") && current.ikunEasterEgg) {
                                    cir.setReturnValue(Component.translatable("subtitles.entity.kun." + subKey).setStyle(subtitleText.getStyle().withColor(TextColor.fromRgb(0xAAAAAA)).withBold(true)));
                                } else {
                                    applyColor(cir, subtitleText, "entity.mob", "passive",
                                            current.colorSettings.entity.mob.passive, defaults.colorSettings.entity.mob.passive);
                                }
                                return;
                            }
                            if (SplitKeyArrays.neutralMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity.mob", "neutral",
                                        current.colorSettings.entity.mob.neutral, defaults.colorSettings.entity.mob.neutral);
                                return;
                            }
                            if (SplitKeyArrays.hostileMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity.mob", "hostile",
                                        current.colorSettings.entity.mob.hostile, defaults.colorSettings.entity.mob.hostile);
                                return;
                            }
                            if (SplitKeyArrays.bossMobsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity.mob", "boss",
                                        current.colorSettings.entity.mob.boss, defaults.colorSettings.entity.mob.boss);
                                return;
                            }
                            if (SplitKeyArrays.vehiclesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity", "vehicle",
                                        current.colorSettings.entity.vehicle, defaults.colorSettings.entity.vehicle);
                                return;
                            }
                            if (SplitKeyArrays.projectilesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity", "projectile",
                                        current.colorSettings.entity.projectile, defaults.colorSettings.entity.projectile);
                                return;
                            }
                            if (SplitKeyArrays.explosivesSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity", "explosive",
                                        current.colorSettings.entity.explosive, defaults.colorSettings.entity.explosive);
                                return;
                            }
                            if (SplitKeyArrays.decorationsSet.contains(entityKey)) {
                                applyColor(cir, subtitleText, "entity", "decoration",
                                        current.colorSettings.entity.decoration, defaults.colorSettings.entity.decoration);
                                return;
                            }
                            applyColor(cir, subtitleText, "entity", "other",
                                    current.colorSettings.entity.other, defaults.colorSettings.entity.other);
                            return;
                        }
                        case "event" -> {
                            applyColor(cir, subtitleText, "entity.mob", "hostile",
                                    current.colorSettings.entity.mob.hostile, defaults.colorSettings.entity.mob.hostile);
                            return;
                        }
                        case "item" -> {
                            if (keyParts.length < 3) {
                                applyColor(cir, subtitleText, "item", "other",
                                        current.colorSettings.item.other, defaults.colorSettings.item.other);
                                return;
                            }
                            String itemKey = keyParts[2];

                            if (SplitKeyArrays.weaponsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, "item", "weapon",
                                        current.colorSettings.item.weapon, defaults.colorSettings.item.weapon);
                                return;
                            }
                            if (SplitKeyArrays.armorsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, "item", "armor",
                                        current.colorSettings.item.armor, defaults.colorSettings.item.armor);
                                return;
                            }
                            if (SplitKeyArrays.toolsSet.contains(itemKey)) {
                                applyColor(cir, subtitleText, "item", "tool",
                                        current.colorSettings.item.tool, defaults.colorSettings.item.tool);
                                return;
                            }
                            applyColor(cir, subtitleText, "item", "other",
                                    current.colorSettings.item.other, defaults.colorSettings.item.other);
                            return;
                        }
                        default -> {
                        }
                    }
                }
            }
            long apiStart = System.nanoTime();
            Component processedText = SubtitleAPI.processSubtitle(subtitleText, Manager.settings);
            long apiTime = System.nanoTime() - apiStart;
            if (apiTime > 100000) {
                PERF_LOGGER.warn("SubtitleAPI.processSubtitle took {}us", apiTime / 1000);
            }

            ColorCode fallback = resolve("", "other",
                    Manager.settings.colorSettings.other, Settings.BUILTIN_DEFAULTS.colorSettings.other);
            cir.setReturnValue(processedText.copy().withStyle(fallback.getFormatting()));
        } finally {
            long elapsed = System.nanoTime() - startTime;

            synchronized (SubtitleEntryMixin.class) {
                totalTime += elapsed;
                callCount++;
                maxTime = Math.max(maxTime, elapsed);
                minTime = Math.min(minTime, elapsed);

                long now = System.currentTimeMillis();
                if (now - lastLogTime >= LOG_INTERVAL) {
                    if (callCount > 0) {
                        double avgTime = (double) totalTime / callCount / 1000;
                        PERF_LOGGER.info("Subtitle colorize stats - Calls: {}, Avg: {}us, Min: {}us, Max: {}us",
                            callCount, String.format("%.2f", avgTime), minTime / 1000, maxTime / 1000);
                    }
                    totalTime = 0;
                    callCount = 0;
                    maxTime = 0;
                    minTime = Long.MAX_VALUE;
                    lastLogTime = now;
                }

                if (elapsed > 1000000) {
                    PERF_LOGGER.warn("colorizeSubtitle took {}ms - potential performance issue", elapsed / 1000000);
                }
            }
        }
    }

    private void applyColor(CallbackInfoReturnable<Component> cir, MutableComponent subtitleText,
                            String path, String key, ColorCode configured, ColorCode builtin) {
        ColorCode resolved = resolve(path, key, configured, builtin);
        cir.setReturnValue(subtitleText.withStyle(resolved.getFormatting()));
    }
}
