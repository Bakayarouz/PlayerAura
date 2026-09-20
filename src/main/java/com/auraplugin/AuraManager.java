package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuraManager {

    private final AuraPlugin plugin;
    private final NamespacedKey auraPdcKey;
    
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private final Map<UUID, BukkitTask> expirationTasks = new HashMap<>();
    
    private final Map<UUID, Boolean> hideSelf = new HashMap<>();
    private final Map<UUID, Boolean> hideOthers = new HashMap<>();

    public AuraManager(AuraPlugin plugin) {
        this.plugin = plugin;
        this.auraPdcKey = new NamespacedKey(plugin, "active_aura");
    }

    public NamespacedKey getPlayerAuraPdcKey() {
        return auraPdcKey;
    }

    public void setAura(Player player, AuraConfig config) {
        removeAura(player, false);

        UUID uuid = player.getUniqueId();
        player.getPersistentDataContainer().set(auraPdcKey, PersistentDataType.STRING, config.getId());

        Location loc = player.getLocation();
        
        ItemDisplay display = player.getWorld().spawn(loc, ItemDisplay.class, entity -> {
            entity.setPersistent(false); // Safeguard: prevents orphan entities on crash
            entity.setBillboard(config.getBillboard());
            entity.setTransformation(new Transformation(
                    new Vector3f(config.getOffsetX(), config.getOffsetY(), config.getOffsetZ()),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(config.getScale(), config.getScale(), config.getScale()),
                    new AxisAngle4f(0, 0, 0, 1)
            ));
        });

        updateDisplayItem(display, config, 0);
        activeAuras.put(uuid, display);

        // Mount as passenger using the original clean mechanics
        player.addPassenger(display);

        if (config.getFrames().size() > 1) {
            final BukkitTask[] taskHolder = new BukkitTask[1];
            taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
                int frameIndex = 0;
                @Override
                public void run() {
                    if (!player.isOnline() || !activeAuras.containsKey(uuid)) {
                        if (taskHolder[0] != null) taskHolder[0].cancel();
                        animationTasks.remove(uuid);
                        return;
                    }
                    frameIndex = (frameIndex + 1) % config.getFrames().size();
                    updateDisplayItem(display, config, frameIndex);
                }
            }, config.getFrameDelay(), config.getFrameDelay());
            animationTasks.put(uuid, taskHolder[0]);
        }
        
        config.getWhenApplied().execute(player);
    }

    public void setTemporaryAura(Player player, AuraConfig config, int seconds) {
        setAura(player, config);
        UUID uuid = player.getUniqueId();
        
        if (expirationTasks.containsKey(uuid)) {
            expirationTasks.get(uuid).cancel();
        }

        BukkitTask expTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                removeAura(player);
                player.sendMessage(plugin.getMessageManager().get("actions.aura-expired"));
            }
        }, seconds * 20L);

        expirationTasks.put(uuid, expTask);
    }

    public void removeAura(Player player) {
        removeAura(player, true);
    }

    private void removeAura(Player player, boolean clearPdc) {
        UUID uuid = player.getUniqueId();
        if (clearPdc) {
            player.getPersistentDataContainer().remove(auraPdcKey);
        }

        if (activeAuras.containsKey(uuid)) {
            ItemDisplay display = activeAuras.get(uuid);
            if (display != null) {
                player.removePassenger(display);
                display.remove();
            }
            activeAuras.remove(uuid);
        }

        if (animationTasks.containsKey(uuid)) {
            animationTasks.get(uuid).cancel();
            animationTasks.remove(uuid);
        }

        if (expirationTasks.containsKey(uuid)) {
            expirationTasks.get(uuid).cancel();
            expirationTasks.remove(uuid);
        }
    }

    public void removeAllAuras() {
        for (UUID uuid : activeAuras.keySet()) {
            ItemDisplay display = activeAuras.get(uuid);
            if (display != null) display.remove();
        }
        activeAuras.clear();
        for (BukkitTask task : animationTasks.values()) task.cancel();
        animationTasks.clear();
        for (BukkitTask task : expirationTasks.values()) task.cancel();
        expirationTasks.clear();
    }

    public void validateActiveAurasOnReload() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            String savedId = player.getPersistentDataContainer().get(auraPdcKey, PersistentDataType.STRING);
            if (savedId != null) {
                AuraConfig config = plugin.getAuraConfigs().get(savedId.toLowerCase());
                if (config == null) {
                    removeAura(player);
                    player.sendMessage(plugin.getMessageManager().get("errors.aura-removed-by-reload"));
                } else {
                    setAura(player, config);
                }
            }
        }
    }

    private void updateDisplayItem(ItemDisplay display, AuraConfig config, int frameIndex) {
        if (display.isDead()) return;
        if (config.getFrames().isEmpty()) return;
        NamespacedKey modelKey = config.getFrames().get(frameIndex % config.getFrames().size());
        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setItemModel(modelKey);
            item.setItemMeta(meta);
        }
        display.setItemStack(item);
    }

    public boolean toggleSelf(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = hideSelf.getOrDefault(uuid, false);
        boolean next = !current;
        hideSelf.put(uuid, next);
        
        ItemDisplay display = activeAuras.get(uuid);
        if (display != null) {
            if (next) player.hideEntity(plugin, display);
            else player.showEntity(plugin, display);
        }
        return next;
    }

    public boolean toggleOthers(Player player) {
        UUID uuid = player.getUniqueId();
        boolean current = hideOthers.getOrDefault(uuid, false);
        boolean next = !current;
        hideOthers.put(uuid, next);

        Map<UUID, ItemDisplay> snapshot = new HashMap<>(activeAuras);
        for (Map.Entry<UUID, ItemDisplay> entry : snapshot.entrySet()) {
            if (!entry.getKey().equals(uuid)) {
                ItemDisplay display = entry.getValue();
                if (display != null && display.isValid()) {
                    if (next) player.hideEntity(plugin, display);
                    else player.showEntity(plugin, display);
                }
            }
        }
        return next;
    }

    public boolean toggleAll(Player player) {
        UUID uuid = player.getUniqueId();
        boolean selfHidden = hideSelf.getOrDefault(uuid, false);
        boolean othersHidden = hideOthers.getOrDefault(uuid, false);
        boolean targetState = !(selfHidden && othersHidden);

        hideSelf.put(uuid, targetState);
        hideOthers.put(uuid, targetState);

        ItemDisplay myDisplay = activeAuras.get(uuid);
        if (myDisplay != null && myDisplay.isValid()) {
            if (targetState) player.hideEntity(plugin, myDisplay);
            else player.showEntity(plugin, myDisplay);
        }

        Map<UUID, ItemDisplay> snapshot = new HashMap<>(activeAuras);
        for (Map.Entry<UUID, ItemDisplay> entry : snapshot.entrySet()) {
            if (!entry.getKey().equals(uuid)) {
                ItemDisplay display = entry.getValue();
                if (display != null && display.isValid()) {
                    if (targetState) player.hideEntity(plugin, display);
                    else player.showEntity(plugin, display);
                }
            }
        }
        return targetState;
    }
}
