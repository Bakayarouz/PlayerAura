package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuraManager {
    private final JavaPlugin plugin;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();

    public AuraManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Spawns or updates an aura for the given player based on configuration.
     */
    public void setAura(Player player, AuraConfig config) {
        UUID uuid = player.getUniqueId();
        removeAura(uuid); // Clean up existing aura safely before setting a new one

        if (config == null || config.getFrames() == null || config.getFrames().isEmpty()) {
            return;
        }

        Location loc = player.getLocation().add(0, 2.2, 0);
        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, entity -> {
            entity.setPersistent(false);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setItemStack(config.getFrames().get(0));
        });

        // Attach display entity as passenger so it smoothly follows player movement
        try {
            player.addPassenger(display);
        } catch (Exception ignored) {
            // Fallback if passenger attachment fails on specific server configurations
        }

        activeAuras.put(uuid, display);

        // Handle multi-frame animations safely with task reference holder
        if (config.getFrames().size() > 1) {
            final BukkitTask[] taskHolder = new BukkitTask[1];
            taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int frameIndex = 0;

                @Override
                public void run() {
                    // Safety check: if player quit or aura was removed, cancel self
                    if (!player.isOnline() || !activeAuras.containsKey(uuid)) {
                        if (taskHolder[0] != null) {
                            taskHolder[0].cancel();
                        }
                        animationTasks.remove(uuid);
                        return;
                    }

                    frameIndex = (frameIndex + 1) % config.getFrames().size();
                    updateDisplayItem(display, config, frameIndex);
                }
            }, config.getFrameDelay(), config.getFrameDelay());

            animationTasks.put(uuid, taskHolder[0]);
        }
    }

    /**
     * Updates the ItemDisplay entity with the next frame item.
     */
    private void updateDisplayItem(ItemDisplay display, AuraConfig config, int frameIndex) {
        if (display != null && display.isValid() && config.getFrames().size() > frameIndex) {
            ItemStack item = config.getFrames().get(frameIndex);
            if (item != null) {
                display.setItemStack(item);
            }
        }
    }

    /**
     * Removes an active aura and cancels its animation task for a specific player.
     */
    public void removeAura(UUID uuid) {
        BukkitTask task = animationTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        ItemDisplay display = activeAuras.remove(uuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }

    /**
     * Cleans up all active auras across the server (used on plugin disable).
     */
    public void removeAll() {
        for (UUID uuid : new HashMap<>(activeAuras).keySet()) {
            removeAura(uuid);
        }
    }

    /**
     * Checks if a player currently has an active aura displayed.
     */
    public boolean hasAura(UUID uuid) {
        return activeAuras.containsKey(uuid);
    }
}
