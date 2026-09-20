package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public final class AuraPlugin extends JavaPlugin {

    private static AuraPlugin instance;
    private AuraManager auraManager;
    private MessageManager messageManager;
    private final Map<String, AuraConfig> auraConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        
        this.messageManager = new MessageManager(this);
        this.auraManager = new AuraManager(this);
        
        loadAuraConfigs();

        getServer().getPluginManager().registerEvents(new AuraEventListener(this), this);

        AuraCommand auraCommand = new AuraCommand(this);
        if (getCommand("aura") != null) {
            getCommand("aura").setExecutor(auraCommand);
            getCommand("aura").setTabCompleter(auraCommand);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AuraPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        }

        getLogger().info("AuraPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (auraManager != null) {
            auraManager.removeAllAuras();
        }
        getLogger().info("AuraPlugin has been disabled.");
    }

    public void loadAuraConfigs() {
        auraConfigs.clear();
        if (getConfig().isConfigurationSection("auras")) {
            var section = getConfig().getConfigurationSection("auras");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    var auraSec = section.getConfigurationSection(key);
                    if (auraSec != null) {
                        auraConfigs.put(key.toLowerCase(), new AuraConfig(key, auraSec));
                    }
                }
            }
        }
        getLogger().info("Loaded " + auraConfigs.size() + " auras from configuration.");
    }

    public void reloadAuraConfig() {
        reloadConfig();
        loadAuraConfigs();
        if (auraManager != null) {
            auraManager.validateActiveAurasOnReload();
        }
    }

    public static AuraPlugin getInstance() { return instance; }
    public AuraManager getAuraManager() { return auraManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public Map<String, AuraConfig> getAuraConfigs() { return auraConfigs; }
}
