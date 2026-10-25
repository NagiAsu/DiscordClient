package xyz.nagiasu.discordclient;

import net.dv8tion.jda.api.JDA;

/**
 * 不綁平台的 JDA 取得入口。
 * Paper 版與 Velocity 版都在連上 Discord 後填入；別的插件用 {@link #getJDA()} 拿，
 * 不用知道自己跑在哪個平台（Velocity 版插件不能引用 DiscordSpigot，它綁著 Bukkit）。
 */
public final class DiscordClientApi {
    private static volatile JDA jda;

    private DiscordClientApi() {}

    /** 還沒連上、沒設定 token 或連線失敗時是 null */
    public static JDA getJDA() {
        return jda;
    }

    static void setJDA(JDA value) {
        jda = value;
    }
}
