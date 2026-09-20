package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class AuraPlugin extends JavaPlugin {

    private AuraManager auraManager;
    private MessageManager messageManager;
    private final Map<String, AuraConfig> auraConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.messageManager = new MessageManager(this);
        this.auraManager = new AuraManager(this);
        reloadAuraConfig();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AuraPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hook established successfully!");
        }

        AuraCommand cmd = new AuraCommand(this);
        if (getCommand("aura") != null) {
            getCommand("aura").setExecutor(cmd);
            getCommand("aura").setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(new AuraEventListener(this, auraManager), this);
    }

    @Override
    public void onDisable() {
        getServer().getOnlinePlayers().forEach(auraManager::removeAuraDisplayOnly);
    }

    public void reloadAuraConfig() {
        reloadConfig();
        messageManager.reloadMessages();
        auraConfigs.clear();
        ConfigurationSection section = getConfig().getConfigurationSection("auras");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection auraSec = section.getConfigurationSection(key);
                if (auraSec != null) {
                    auraConfigs.put(key.toLowerCase(), new AuraConfig(key, auraSec));
                }
            }
        }

        if (auraManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String savedId = player.getPersistentDataContainer().get(auraManager.getPlayerAuraPdcKey(), PersistentDataType.STRING);
                if (savedId != null) {
                    if (!auraConfigs.containsKey(savedId.toLowerCase())) {
                        auraManager.removeAura(player);
                        player.sendMessage(messageManager.get("actions.config-deleted-removal"));
                    } else {
                        auraManager.reapplyStoredAura(player);
                    }
                }
            }
        }
    }

    public AuraManager getAuraManager() { return auraManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public Map<String, AuraConfig> getAuraConfigs() { return auraConfigs; }
}
