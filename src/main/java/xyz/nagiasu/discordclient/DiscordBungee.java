package xyz.nagiasu.discordclient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import com.google.common.io.ByteStreams;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

public class DiscordBungee extends Plugin {
    Configuration config;
    static JDA jdaApi = null;
    TextChannel tc;

    @Override
    public void onEnable() {
        // DiscordBot
        if (!createConfig()) {
            getLogger().info("config 初始化錯誤! ");
            return;
        }
        if (config.getString("DiscordBotToken").equals("")) {
            this.getLogger().info(ChatColor.RED + "尚未設定Discord Token");
            return;
        }
        // Discord JDA Connect
        try {
            jdaApi = JDABuilder.createDefault(config.getString("DiscordBotToken"))
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL).enableCache(CacheFlag.EMOJI)
                    .enableIntents(GatewayIntent.GUILD_PRESENCES)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
            jdaApi.awaitReady();
            this.getLogger().info(ChatColor.GREEN + "Discord連接成功!");
        } catch (IllegalStateException e) {
            e.printStackTrace();
            jdaApi = null;
            return;
        } catch (InterruptedException e) {
            e.printStackTrace();
            jdaApi = null;
            return;
        }
        // Startup Message
        tc = jdaApi.getTextChannelById(config.getString("GlobalMessageChannel"));
        if (tc != null && !config.getString("StartMessage").equals("")) {
            tc.sendMessage(config.getString("StartMessage")).complete();
        }
    }

    @Override
    public void onDisable() {
        if (tc != null && !config.getString("StopMessage").equals("")) {
            tc.sendMessage(config.getString("StopMessage")).complete();
        }
        jdaApi.shutdownNow();
    }

    boolean createConfig() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdir();
            }
            File configFile = new File(getDataFolder().getPath(), "config.yml");
            if (!configFile.exists()) {
                try {
                    configFile.createNewFile();
                    try (InputStream is = getResourceAsStream("config.yml");
                            OutputStream os = new FileOutputStream(configFile)) {
                        ByteStreams.copy(is, os);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Unable to create configuration file", e);
                }
            }
            this.config =
                    ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        saveConfig();
        return true;
    }

    void saveConfig() {
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config,
                    new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JDA getJDA() {
        return jdaApi;
    }
}
