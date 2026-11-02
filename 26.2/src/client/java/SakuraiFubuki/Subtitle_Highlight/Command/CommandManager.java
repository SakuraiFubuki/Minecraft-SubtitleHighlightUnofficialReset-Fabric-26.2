package SakuraiFubuki.Subtitle_Highlight.Command;

import SakuraiFubuki.Subtitle_Highlight.Configure.Manager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public class CommandManager {
    /**
     * 26.2 的 fabric-api 移除了 ClientCommandManager；
     * 客户端命令直接用 brigadier 的 builder 工厂构造（泛型参数为 FabricClientCommandSource）。
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    private static <T> RequiredArgumentBuilder<FabricClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("subtitle_highlight")
            .then(literal("reload")
                .executes(CommandManager::reloadConfig)
            )
            .then(literal("save")
                .executes(CommandManager::saveConfig)
            )
            .then(literal("maxDuration")
                .then(argument("value", LongArgumentType.longArg(0))
                    .executes(CommandManager::setMaximumDuration)
                )
                .executes(CommandManager::getMaximumDuration)
            )
            .then(literal("scale")
                .then(argument("value", FloatArgumentType.floatArg(0.1f, 5.0f))
                    .executes(CommandManager::setScale)
                )
                .executes(CommandManager::getScale)
            )
            .then(literal("bottomMargin")
                .then(argument("value", FloatArgumentType.floatArg(0))
                    .executes(CommandManager::setBottomMargin)
                )
                .executes(CommandManager::getBottomMargin)
            )
            .then(literal("sideMargin")
                .then(argument("value", FloatArgumentType.floatArg(0))
                    .executes(CommandManager::setSideMargin)
                )
                .executes(CommandManager::getSideMargin)
            )
            .then(literal("backgroundColor")
                // 背景色是 ARGB int，0xcc000000 这类值在 Java 里是负数，所以必须放开到整个 int 区间；
                // 写成 integer(0, 0xffffffff) 会得到 [0, -1] 这种非法区间，参数永远无法通过校验。
                .then(argument("value", IntegerArgumentType.integer())
                    .executes(CommandManager::setBackgroundColor)
                )
                .executes(CommandManager::getBackgroundColor)
            )
            .then(literal("startRatio")
                .then(argument("value", FloatArgumentType.floatArg(0, 1))
                    .executes(CommandManager::setStartRatio)
                )
                .executes(CommandManager::getStartRatio)
            )
            .then(literal("endRatio")
                .then(argument("value", FloatArgumentType.floatArg(0, 1))
                    .executes(CommandManager::setEndRatio)
                )
                .executes(CommandManager::getEndRatio)
            )
            .then(literal("ikunEasterEgg")
                .then(argument("value", StringArgumentType.string())
                    .executes(CommandManager::setIkuneasterEgg)
                )
                .executes(CommandManager::getIkuneasterEgg)
            )
            .executes(CommandManager::showHelp)
        );
    }

    private static int reloadConfig(CommandContext<FabricClientCommandSource> context) {
        Manager.load();
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.reload.success"));
        return 1;
    }

    private static int saveConfig(CommandContext<FabricClientCommandSource> context) {
        Manager.save();
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.save.success"));
        return 1;
    }

    private static int setMaximumDuration(CommandContext<FabricClientCommandSource> context) {
        long value = LongArgumentType.getLong(context, "value");
        Manager.settings.maxDuration = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.maxDuration.set", value));
        return 1;
    }

    private static int getMaximumDuration(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.maxDuration.get", Manager.settings.maxDuration));
        return 1;
    }

    private static int setScale(CommandContext<FabricClientCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        Manager.settings.scale = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.scale.set", value));
        return 1;
    }

    private static int getScale(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.scale.get", Manager.settings.scale));
        return 1;
    }

    private static int setBottomMargin(CommandContext<FabricClientCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        Manager.settings.bottomMargin = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.bottomMargin.set", value));
        return 1;
    }

    private static int getBottomMargin(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.bottomMargin.get", Manager.settings.bottomMargin));
        return 1;
    }

    private static int setSideMargin(CommandContext<FabricClientCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        Manager.settings.sideMargin = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.sideMargin.set", value));
        return 1;
    }

    private static int getSideMargin(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.sideMargin.get", Manager.settings.sideMargin));
        return 1;
    }

    private static int setBackgroundColor(CommandContext<FabricClientCommandSource> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        Manager.settings.backgroundColor = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.backgroundColor.set", Integer.toHexString(value)));
        return 1;
    }

    private static int getBackgroundColor(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.backgroundColor.get", Integer.toHexString(Manager.settings.backgroundColor)));
        return 1;
    }

    private static int setStartRatio(CommandContext<FabricClientCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        Manager.settings.startRatio = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.startRatio.set", value));
        return 1;
    }

    private static int getStartRatio(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.startRatio.get", Manager.settings.startRatio));
        return 1;
    }

    private static int setEndRatio(CommandContext<FabricClientCommandSource> context) {
        float value = FloatArgumentType.getFloat(context, "value");
        Manager.settings.endRatio = value;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.endRatio.set", value));
        return 1;
    }

    private static int getEndRatio(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.endRatio.get", Manager.settings.endRatio));
        return 1;
    }

    private static int setIkuneasterEgg(CommandContext<FabricClientCommandSource> context) {
        String value = StringArgumentType.getString(context, "value");
        boolean boolValue = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("yes");
        Manager.settings.ikunEasterEgg = boolValue;
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.ikunEasterEgg.set", boolValue ? Component.translatable("subtitle_highlight.command.enabled") : Component.translatable("subtitle_highlight.command.disabled")));
        return 1;
    }

    private static int getIkuneasterEgg(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.ikunEasterEgg.get", Manager.settings.ikunEasterEgg ? Component.translatable("subtitle_highlight.command.enabled") : Component.translatable("subtitle_highlight.command.disabled")));
        return 1;
    }

    private static int showHelp(CommandContext<FabricClientCommandSource> context) {
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.title"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.reload"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.save"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.maxDuration"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.scale"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.bottomMargin"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.sideMargin"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.backgroundColor"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.startRatio"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.endRatio"));
        context.getSource().sendFeedback(Component.translatable("subtitle_highlight.command.help.ikunEasterEgg"));
        return 1;
    }
}
