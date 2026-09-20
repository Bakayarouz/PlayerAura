package com.auraplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class AuraEventListener implements Listener {
    private final AuraManager manager;

    public AuraEventListener(AuraManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { manager.removeAura(e.getPlayer()); }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) { manager.removeAura(e.getEntity()); }
}
