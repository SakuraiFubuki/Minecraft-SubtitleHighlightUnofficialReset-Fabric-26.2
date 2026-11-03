package SakuraiFubuki.Subtitle_Highlight.Mixin;

import SakuraiFubuki.Subtitle_Highlight.Configure.Manager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.SubtitleOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.2 的字幕渲染整套重写过（原 {@code SubtitlesHud.render(DrawContext)} 拆成了
 * {@code SubtitleOverlay.extractRenderState(GuiGraphicsExtractor)}，只往 GuiRenderState 里塞状态），
 * 所以这里按新的调用点重做注入，逻辑与原 1.21.x 版本一致：
 * <ol>
 *   <li>显示时长：{@code purgeOldInstances(3000ms × 通知倍率)} → {@code maxDuration × 通知倍率}</li>
 *   <li>淡出进度：{@code elapsed/(3000×倍率)} → {@code elapsed/(maxDuration×倍率)}</li>
 *   <li>方向指示亮度：startRatio → endRatio 线性插值</li>
 *   <li>背景色、缩放、边距位移</li>
 *   <li>字幕文字颜色：取字幕样式色，按淡出比例压暗后交给 GuiGraphicsExtractor</li>
 * </ol>
 */
@Environment(EnvType.CLIENT)
@Mixin(SubtitleOverlay.class)
public class SubtitleOverlayMixin {
    private static final String TARGET_METHOD = "extractRenderState";

    /** 当前这条字幕的淡出进度（0 = 刚出现，1 = 即将消失）。 */
    @Unique
    private static float subtitle_highlight$durationRatio;

    /**
     * 显示时长。原版传进来的是 {@code 3000.0 * notificationDisplayTime}，
     * 这里按比例换成 {@code maxDuration * notificationDisplayTime}。
     */
    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/SubtitleOverlay$Subtitle;purgeOldInstances(D)V"),
            index = 0)
    private double subtitle_highlight$modifySubtitleLifetime(double originalLifetime) {
        double maxDuration = Math.max(0, Manager.settings.maxDuration);
        return originalLifetime * maxDuration / 3000.0;
    }

    /**
     * 淡出进度。原版是 {@code (now - time) / (3000.0 * multiplier)}，
     * 这里等比例换成 {@code maxDuration} 作分母，并把结果缓存给同一条字幕后续的
     * 箭头发亮度与文字颜色使用（同一次循环内先算插值再画文字，顺序固定）。
     */
    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clampedLerp(FFF)F"),
            index = 0)
    private float subtitle_highlight$modifyFadeProgress(float originalProgress) {
        double maxDuration = Math.max(1, Manager.settings.maxDuration);
        float progress = (float) (originalProgress * 3000.0 / maxDuration);
        subtitle_highlight$durationRatio = Math.max(0.0F, Math.min(1.0F, progress));
        return progress;
    }

    /**
     * 方向指示箭头（&lt; / &gt;）的亮度：原版写死 255 → 75，这里改用 startRatio → endRatio。
     *
     * <p>注意参数顺序：mojmap 是 {@code Mth.clampedLerp(factor, min, max)}，
     * 与 Yarn 的 {@code clampedLerp(min, max, delta)} 正好相反 —— 按 Yarn 顺序传会把
     * 255 当成插值进度、把 0~1 的进度当成返回值，亮度直接变成 0（纯黑）。</p>
     */
    @Redirect(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(F)I"))
    private int subtitle_highlight$modifyIndicatorBrightness(float value) {
        return Mth.floor(Mth.clampedLerp(
                subtitle_highlight$durationRatio,
                (float) (255 * Manager.settings.startRatio),
                (float) (255 * Manager.settings.endRatio)));
    }

    /** 字幕背景色。 */
    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"),
            index = 4)
    private int subtitle_highlight$modifyBackgroundColor(int originalColor) {
        return Manager.settings.backgroundColor;
    }

    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;"),
            index = 0)
    private float subtitle_highlight$modifyScaleX(float originalScale) {
        return Manager.settings.scale;
    }

    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;scale(FF)Lorg/joml/Matrix3x2f;"),
            index = 1)
    private float subtitle_highlight$modifyScaleY(float originalScale) {
        return Manager.settings.scale;
    }

    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"),
            index = 0)
    private float subtitle_highlight$modifyTranslateX(float originalX) {
        float scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float width = -(originalX - scaledWidth + 2);
        return scaledWidth - (width + 1) * Math.abs(Manager.settings.scale) - Manager.settings.sideMargin;
    }

    @ModifyArg(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;translate(FF)Lorg/joml/Matrix3x2f;"),
            index = 1)
    private float subtitle_highlight$modifyTranslateY(float originalY) {
        float scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        float height = -(originalY - scaledHeight + 35);
        return scaledHeight - (height + 5) * Math.abs(Manager.settings.scale) - Manager.settings.bottomMargin;
    }

    /**
     * 同种字幕要不要分行显示。
     *
     * <p>原版 {@code onPlaySound} 用 {@code subtitle.getText().equals(text)} 判断「这条字幕已经有了」，
     * 命中就 {@code refresh()} 刷新计时而不是新开一行。但本模组会给字幕文本套样式色
     * （见 {@link SubtitleEntryMixin}），带样式的新组件与原版文本不相等，于是原版去重永远失配
     * ——同一种字幕的每个声音源都会单独占一行。</p>
     *
     * <p>这里在「合并模式」（{@code separateSameSubtitle = false}，默认值）下把这次比较改成按可见文本比，
     * 让原版去重重新生效：同种字幕只占一行。开启开关时才保持原样的严格比较，恢复分行行为。</p>
     */
    @Redirect(method = "onPlaySound",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;equals(Ljava/lang/Object;)Z"))
    private boolean subtitle_highlight$compareSubtitleText(Component self, Object other) {
        if (Boolean.TRUE.equals(Manager.settings.separateSameSubtitle)) {
            return self.equals(other);
        }
        return subtitle_highlight$visibleText(self).equals(subtitle_highlight$visibleText(other));
    }

    /** 取字幕的可见文本（翻译已解析、样式忽略），用于「合并模式」下的同种判重。 */
    @Unique
    private static String subtitle_highlight$visibleText(Object component) {
        return component instanceof Component c ? c.getString() : String.valueOf(component);
    }

    /**
     * 字幕文字。原版把整段文字当成纯白并按亮度压暗，这里改为：
     * 取字幕自身样式颜色（由 {@link SubtitleEntryMixin} 按读音类型着色），
     * 按同一淡出进度插值后作为最终 ARGB；文字本身去掉样式色以免覆盖 int 颜色。
     */
    @Redirect(method = TARGET_METHOD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void subtitle_highlight$modifySubtitleText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int color) {
        float ratio = subtitle_highlight$durationRatio;

        int red = 255;
        int green = 255;
        int blue = 255;
        TextColor styleColor = text.getStyle().getColor();
        if (styleColor != null) {
            int rgb = styleColor.getValue();
            red = (rgb >> 16) & 255;
            green = (rgb >> 8) & 255;
            blue = rgb & 255;
        }

        // mojmap 的 clampedLerp 是 (factor, min, max)，Yarn 是 (min, max, delta)：顺序不能照抄
        float startRatio = (float) Manager.settings.startRatio;
        float endRatio = (float) Manager.settings.endRatio;
        int redRemaining = Mth.floor(Mth.clampedLerp(ratio, red * startRatio, red * endRatio));
        int greenRemaining = Mth.floor(Mth.clampedLerp(ratio, green * startRatio, green * endRatio));
        int blueRemaining = Mth.floor(Mth.clampedLerp(ratio, blue * startRatio, blue * endRatio));

        MutableComponent plain = Component.literal(text.getString()).setStyle(text.getStyle().withColor((TextColor) null));

        graphics.text(font, plain, x, y, ARGB.color(255, redRemaining, greenRemaining, blueRemaining));
    }
}
