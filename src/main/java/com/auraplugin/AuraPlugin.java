package com.auraplugin;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class AuraPlugin extends JavaPlugin {

    private AuraManager auraManager;
    private final Map<String, AuraConfig> auraConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.auraManager = new AuraManager(this);
        reloadAuraConfig();

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
    }

    public AuraManager getAuraManager() { return auraManager; }
    public Map<String, AuraConfig> getAuraConfigs() { return auraConfigs; }
}
