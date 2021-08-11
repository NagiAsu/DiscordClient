package xyz.nagiasu.discordclient;

import javax.security.auth.login.LoginException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.md_5.bungee.api.ChatColor;

public class DiscordSp extends JavaPlugin {
    FileConfiguration config;
    static JDA jdaApi = null;

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().copyDefaults(true);
        config.addDefault("DiscordBotToken", "");
        saveConfig();
        reloadConfig();
        if (config.getString("DiscordBotToken").equals("")) {
            this.getLogger().info(ChatColor.RED + "尚未設定Discord Token");
            this.getPluginLoader().disablePlugin(this);
            return;
        }
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
    }

    public static JDA getJDA() {
        return jdaApi;
    }
}
