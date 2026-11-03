package SakuraiFubuki.Subtitle_Highlight.Configure;

import SakuraiFubuki.Subtitle_Highlight.Util.ColorCode;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;

public class Settings {
    /**
     * 内置默认值快照。用来判断某项颜色「用户是否改过」：
     * 改过 → 以配置为准；保持默认 → 让数据包（data/subtitle_highlight/subtitle_types.json）说话。
     */
    public static final Settings BUILTIN_DEFAULTS = new Settings();

    public long maxDuration = 3000;
    public double startRatio = 1;
    public double endRatio = 0.29411764705882354;
    public int backgroundColor = 0xcc000000;
    public float scale = 1;
    public float bottomMargin = 30;
    public float sideMargin = 1;
    public Boolean ikunEasterEgg = true;
    /**
     * 同种字幕是否分行显示（每个声音源各占一行）。
     *
     * <p>{@code false}（默认）：同一种字幕只显示一行，重复触发时刷新计时而不再新开一行。</p>
     * <p>{@code true}：同一种字幕的每个声音源都单独占一行（移植后曾作为默认行为）。</p>
     */
    public Boolean separateSameSubtitle = false;
    public ColorSetting colorSettings = new ColorSetting();
    public ArrayList<Custom> customList = new ArrayList<>();

    public class ColorSetting {
        public ColorCode ambient = ColorCode.DARK_BLUE;
        public Block block = new Block();
        public ColorCode enchant = ColorCode.DARK_PURPLE;
        public Entity entity = new Entity();
        public Item item = new Item();
        public ColorCode other = ColorCode.WHITE;

        public class Item {
            public ColorCode weapon = ColorCode.GOLD;
            public ColorCode armor = ColorCode.DARK_GREEN;
            public ColorCode tool = ColorCode.BLUE;
            public ColorCode other = ColorCode.WHITE;
        }

        public class Block {
            public ColorCode generic = ColorCode.WHITE;
            public ColorCode interact = ColorCode.AQUA;
            public ColorCode working = ColorCode.DARK_AQUA;
            public ColorCode dangerous = ColorCode.RED;
            public ColorCode crop = ColorCode.DARK_GREEN;
            public ColorCode other = ColorCode.DARK_GRAY;
        }

        public class Entity {
            public Mob mob = new Mob();
            public ColorCode vehicle = ColorCode.GRAY;
            public ColorCode projectile = ColorCode.GOLD;
            public ColorCode explosive = ColorCode.RED;
            public ColorCode decoration = ColorCode.GRAY;
            public ColorCode other = ColorCode.DARK_GRAY;

            public class Mob {

                public Player player = new Player();
                public ColorCode passive = ColorCode.GREEN;
                public ColorCode neutral = ColorCode.YELLOW;
                public ColorCode hostile = ColorCode.RED;
                public ColorCode boss = ColorCode.LIGHT_PURPLE;

                public class Player {
                    public ColorCode attack = ColorCode.GOLD;
                    public ColorCode hurt = ColorCode.DARK_RED;
                    public ColorCode other = ColorCode.WHITE;
                }
            }
        }
    }

    public class Custom {
        public String translationKey = "";
        public TextColor color = TextColor.fromRgb(0xffffff);
        public Boolean obfuscated = false;
        public Boolean bold = false;
        public Boolean strikethrough = false;
        public Boolean underline = false;
        public Boolean italic = false;
    }
}
