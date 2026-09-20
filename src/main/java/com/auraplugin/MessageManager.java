package com.auraplugin;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MessageManager {
    private final AuraPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessageManager(AuraPlugin plugin) {
        this.plugin = plugin;
        reloadMessages();
    }

    public void reloadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defConfig);
        }
    }

    public String get(String path, Map<String, String> placeholders) {
        String prefix = messagesConfig.getString("prefix", "&8[&6Aura&8] ");
        String message = messagesConfig.getString(path, "&cMissing message path: " + path);
        
        message = message.replace("{prefix}", prefix);
        
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String get(String path) {
        return get(path, null);
    }
}
