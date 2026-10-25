package xyz.nagiasu.discordclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;
import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * Velocity（proxy）版：連上 Discord，把 JDA 放進 {@link DiscordClientApi} 給 proxy 上的其他插件用。
 * 別的插件要在自己的 ProxyInitializeEvent（預設順序）裡拿 JDA；這裡用 PostOrder.FIRST 先連好。
 */
public class DiscordVelocity {
    private final Logger logger;
    private final Path dataFolder;
    private Map<String, Object> config;
    private JDA jdaApi;
    private TextChannel tc;

    @Inject
    public DiscordVelocity(Logger logger, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        // 設定沿用 Waterfall 時代的 plugins/DiscordClient/（Velocity 預設的資料夾是小寫的插件 id）
        this.dataFolder = dataDirectory.resolveSibling("DiscordClient");
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onProxyInitialize(ProxyInitializeEvent event) {
        if (!loadConfig()) {
            logger.info("config 初始化錯誤! ");
            return;
        }
        String token = getString("DiscordBotToken");
        if (token.isEmpty()) {
            logger.info("尚未設定Discord Token");
            return;
        }
        // Discord JDA Connect
        try {
            jdaApi = JDABuilder.createDefault(token)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL).enableCache(CacheFlag.EMOJI)
                    .enableIntents(GatewayIntent.GUILD_PRESENCES)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
            jdaApi.awaitReady();
            logger.info("Discord連接成功!");
            DiscordClientApi.setJDA(jdaApi);
        } catch (IllegalStateException | InterruptedException e) {
            logger.error("Discord 連線失敗", e);
            jdaApi = null;
            return;
        }
        // Startup Message
        tc = jdaApi.getTextChannelById(getString("GlobalMessageChannel"));
        if (tc != null && !getString("StartMessage").isEmpty()) {
            tc.sendMessage(getString("StartMessage")).complete();
        }
    }

    @Subscribe(order = PostOrder.LAST)
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (jdaApi == null) {
            return;
        }
        if (tc != null && !getString("StopMessage").isEmpty()) {
            tc.sendMessage(getString("StopMessage")).complete();
        }
        DiscordClientApi.setJDA(null);
        jdaApi.shutdownNow();
        // 等 JDA 的執行緒真的結束再讓 proxy 關掉插件的 jar（Paper 版 10-25 實測過「zip file closed」）
        try {
            jdaApi.awaitShutdown(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 只讀設定、不寫回（回滾到 Waterfall 時同一個檔案照舊能用）；沒有檔案才放預設值 */
    @SuppressWarnings("unchecked")
    private boolean loadConfig() {
        Path configFile = dataFolder.resolve("config.yml");
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(dataFolder);
                try (InputStream is = getClass().getResourceAsStream("/config.yml")) {
                    Files.copy(is, configFile);
                }
            }
            try (Reader rd = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                Object loaded = new Yaml().load(rd);
                config = loaded instanceof Map ? (Map<String, Object>) loaded : Map.of();
            }
            return true;
        } catch (IOException | RuntimeException e) {
            logger.error("讀不到 " + configFile, e);
            return false;
        }
    }

    private String getString(String key) {
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
