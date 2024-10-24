package ilbie.com.tailsplugin.utils;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;

public class ColorUtils {
    /**
     * Bukkit의 ChatColor를 Adventure의 NamedTextColor로 변환합니다.
     * 대응되는 색상이 없을 경우 WHITE를 반환합니다.
     *
     * @param chatColor 변환할 ChatColor
     * @return 대응하는 NamedTextColor
     */
    public static NamedTextColor toNamedTextColor(ChatColor chatColor) {
        if (chatColor == null) {
            return NamedTextColor.WHITE;
        }
        switch (chatColor) {
            case BLACK:
                return NamedTextColor.BLACK;
            case DARK_BLUE:
                return NamedTextColor.DARK_BLUE;
            case DARK_GREEN:
                return NamedTextColor.DARK_GREEN;
            case DARK_AQUA:
                return NamedTextColor.DARK_AQUA;
            case DARK_RED:
                return NamedTextColor.DARK_RED;
            case DARK_PURPLE:
                return NamedTextColor.DARK_PURPLE;
            case GOLD:
                return NamedTextColor.GOLD;
            case GRAY:
                return NamedTextColor.GRAY;
            case DARK_GRAY:
                return NamedTextColor.DARK_GRAY;
            case BLUE:
                return NamedTextColor.BLUE;
            case GREEN:
                return NamedTextColor.GREEN;
            case AQUA:
                return NamedTextColor.AQUA;
            case RED:
                return NamedTextColor.RED;
            case LIGHT_PURPLE:
                return NamedTextColor.LIGHT_PURPLE;
            case YELLOW:
                return NamedTextColor.YELLOW;
            case WHITE:
            default:
                return NamedTextColor.WHITE;
        }
    }
}
