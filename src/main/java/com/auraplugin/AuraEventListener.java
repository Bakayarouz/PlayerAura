package com.auraplugin;

import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class AuraEventListener implements Listener {

    private final AuraPlugin plugin;

    public AuraEventListener(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String savedAura = player.getPersistentDataContainer().get(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
        if (savedAura != null) {
            AuraConfig config = plugin.getAuraConfigs().get(savedAura.toLowerCase());
            if (config != null) {
                plugin.getAuraManager().setAura(player, config);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getAuraManager().removeAura(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String savedAura = player.getPersistentDataContainer().get(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
        if (savedAura != null) {
            AuraConfig config = plugin.getAuraConfigs().get(savedAura.toLowerCase());
            if (config != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getAuraManager().setAura(player, config), 5L);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        String savedAura = player.getPersistentDataContainer().get(plugin.getAuraManager().getPlayerAuraPdcKey(), PersistentDataType.STRING);
        if (savedAura != null) {
            AuraConfig config = plugin.getAuraConfigs().get(savedAura.toLowerCase());
            if (config != null) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getAuraManager().setAura(player, config), 2L);
            }
        }
    }
}
