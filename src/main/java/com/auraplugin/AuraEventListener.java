package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class AuraEventListener implements Listener {

    private final AuraPlugin plugin;
    private final AuraManager manager;

    public AuraEventListener(AuraPlugin plugin, AuraManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Delay by 2 ticks so player position and chunks fully initialize
        delayReapply(event.getPlayer(), 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Despawn entity on leave (Player PDC retains saved selection)
        manager.removeAuraDisplayOnly(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        // Despawn entity immediately on death so it doesn't float over the corpse
        manager.removeAuraDisplayOnly(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Re-mount display entity 2 ticks after respawning at bed/spawnpoint
        delayReapply(event.getPlayer(), 2L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        // Passengers dismount during cross-dimension teleports (Nether/End); re-mount after switch
        delayReapply(event.getPlayer(), 2L);
    }

    private void delayReapply(Player player, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                manager.reapplyStoredAura(player);
            }
        }, delayTicks);
    }
}
