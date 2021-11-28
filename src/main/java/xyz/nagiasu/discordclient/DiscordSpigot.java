package xyz.nagiasu.discordclient;

import javax.security.auth.login.LoginException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.md_5.bungee.api.ChatColor;

public class DiscordSpigot extends JavaPlugin {
    FileConfiguration config;
    static JDA jdaApi = null;
    TextChannel tc;

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        config.addDefault("DiscordBotToken", "");
        config.addDefault("GlobalMessageChannel", "");
        config.addDefault("StartMessage", "");
        config.addDefault("StopMessage", "");
        saveConfig();
        reloadConfig();
        if (config.getString("DiscordBotToken").equals("")) {
            this.getLogger().info(ChatColor.RED + "尚未設定Discord Token");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
        // Discord JDA Connect
        try {
            jdaApi = JDABuilder.createDefault(config.getString("DiscordBotToken")).build();
            jdaApi.awaitReady();
            this.getLogger().info(ChatColor.GREEN + "Discord連接成功!");
        } catch (LoginException e) {
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

    public static JDA getJDA() {
        return jdaApi;
    }

    @Override
    public void onDisable() {
        if (tc != null && !config.getString("StopMessage").equals("")) {
            tc.sendMessage(config.getString("StopMessage")).complete();
        }
        jdaApi.shutdownNow();
    }
}
