package com.auraplugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class AuraPlaceholderExpansion extends PlaceholderExpansion {

    private final AuraPlugin plugin;

    public AuraPlaceholderExpansion(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "aura";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("active")) {
            String savedId = player.getPersistentDataContainer().get(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
            return savedId != null ? savedId : "None";
        }

        if (identifier.equalsIgnoreCase("display_name") || identifier.equalsIgnoreCase("display")) {
            String savedId = player.getPersistentDataContainer().get(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
            if (savedId == null) return "None";
            AuraConfig config = plugin.getAuraConfigs().get(savedId.toLowerCase());
            return config != null ? config.getDisplayName() : "None";
        }

        if (identifier.equalsIgnoreCase("has_active")) {
            boolean hasActive = player.getPersistentDataContainer().has(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
            return String.valueOf(hasActive);
        }

        return null;
    }
}
