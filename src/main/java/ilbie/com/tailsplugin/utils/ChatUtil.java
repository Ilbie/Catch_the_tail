package ilbie.com.tailsplugin.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ChatUtil {

    /**
     * 메시지 앞에 공통 접두사를 추가합니다.
     *
     * @return 접두사 컴포넌트
     */
    public static Component prefix() {
        return Component.text("[TailsPlugin] ", NamedTextColor.AQUA);
    }
}
