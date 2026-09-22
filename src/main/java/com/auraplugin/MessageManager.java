package com.auraplugin;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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
        try (InputStreamReader defConfigStream =
                     new InputStreamReader(plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration((Reader) defConfigStream);
            messagesConfig.setDefaults((Configuration) defConfig);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load default messages.yml");
        }
    }

    /**
     * Returns true if the message at this path is unset or an empty string,
     * meaning it should be treated as "send nothing" rather than a real message.
     */
    public boolean isBlank(String path) {
        String message = messagesConfig.getString(path, "");
        return message == null || message.isEmpty();
    }

    public Component get(String path) {
        return get(path, null);
    }

    public Component get(String path, Map<String, String> placeholders) {
        String prefix = messagesConfig.getString("prefix", "&8[&6Aura&8] ");
        String message = messagesConfig.getString(path, "&cMissing message: " + path);
        message = message.replace("{prefix}", prefix);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return LegacyComponentSerializer.legacy('&').deserialize(message);
    }

    /**
     * Sends the message at this path to the recipient, unless it is blank ("" or unset),
     * in which case nothing is sent.
     */
    public void send(org.bukkit.command.CommandSender recipient, String path) {
        send(recipient, path, null);
    }

    public void send(org.bukkit.command.CommandSender recipient, String path, Map<String, String> placeholders) {
        if (isBlank(path)) {
            return;
        }
        recipient.sendMessage(get(path, placeholders));
    }
}
