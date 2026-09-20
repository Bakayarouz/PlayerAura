package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public class AuraManager {

    private final AuraPlugin plugin;
    private final NamespacedKey playerAuraPdcKey;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();
    private final Map<UUID, BukkitTask> temporaryTasks = new HashMap<>();
    private final Map<UUID, String> hijackedAuraBackups = new HashMap<>();
    
    private final Map<UUID, BukkitTask> animationTasks = new HashMap<>();
    private final Map<UUID, Integer> frameIndices = new HashMap<>();
    private final Set<UUID> auraVisibilityDisabled = new HashSet<>();

    public AuraManager(AuraPlugin plugin) {
        this.plugin = plugin;
        this.playerAuraPdcKey = new NamespacedKey(plugin, "selected_aura_id");
    }

    NamespacedKey getPlayerAuraPdcKey() {
        return playerAuraPdcKey;
    }

    public boolean toggleAuraVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        if (auraVisibilityDisabled.contains(uuid)) {
            auraVisibilityDisabled.remove(uuid);
            for (ItemDisplay display : activeAuras.values()) {
                if (display.isValid()) {
                    player.showEntity(plugin, display);
                }
            }
            return true;
        } else {
            auraVisibilityDisabled.add(uuid);
            for (ItemDisplay display : activeAuras.values()) {
                if (display.isValid()) {
                    player.hideEntity(plugin, display);
                }
            }
            return false;
        }
    }

    public void setAura(Player player, AuraConfig newConfig) {
        String oldSavedId = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
        if (oldSavedId != null) {
            AuraConfig oldConfig = plugin.getAuraConfigs().get(oldSavedId.toLowerCase());
            if (oldConfig != null && !oldConfig.getId().equals(newConfig.getId())) {
                oldConfig.getWhenDisabled().execute(player);
            }
        }

        clearTempTask(player.getUniqueId());
        hijackedAuraBackups.remove(player.getUniqueId());

        player.getPersistentDataContainer().set(playerAuraPdcKey, PersistentDataType.STRING, newConfig.getId());
        spawnAuraDisplay(player, newConfig);
        newConfig.getWhenApplied().execute(player);
    }

    public void setTemporaryAura(Player player, AuraConfig config, int durationSeconds) {
        UUID uuid = player.getUniqueId();
        clearTempTask(uuid);

        if (!hijackedAuraBackups.containsKey(uuid)) {
            String currentSaved = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
            hijackedAuraBackups.put(uuid, currentSaved);
        }

        spawnAuraDisplay(player, config);
        config.getWhenApplied().execute(player);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            temporaryTasks.remove(uuid);
            if (!player.isOnline()) return;
            revertTemporaryAura(player);
        }, durationSeconds * 20L);

        temporaryTasks.put(uuid, task);
    }

    public void revertTemporaryAura(Player player) {
        UUID uuid = player.getUniqueId();
        clearTempTask(uuid);

        String tempSavedId = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
        if (tempSavedId != null) {
            AuraConfig tempConfig = plugin.getAuraConfigs().get(tempSavedId.toLowerCase());
            if (tempConfig != null) tempConfig.getWhenDisabled().execute(player);
        }

        String previousAuraId = hijackedAuraBackups.remove(uuid);
        if (previousAuraId != null) {
            player.getPersistentDataContainer().set(playerAuraPdcKey, PersistentDataType.STRING, previousAuraId);
            AuraConfig oldConfig = plugin.getAuraConfigs().get(previousAuraId.toLowerCase());
            if (oldConfig != null) {
                spawnAuraDisplay(player, oldConfig);
                oldConfig.getWhenApplied().execute(player);
            } else {
                removeAuraDisplayOnly(player);
            }
        } else {
            removeAura(player);
        }
    }

    private void clearTempTask(UUID uuid) {
        BukkitTask task = temporaryTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void removeAura(Player player) {
        String savedId = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
        if (savedId != null) {
            AuraConfig config = plugin.getAuraConfigs().get(savedId.toLowerCase());
            if (config != null) {
                config.getWhenDisabled().execute(player);
            }
        }

        clearTempTask(player.getUniqueId());
        hijackedAuraBackups.remove(player.getUniqueId());
        player.getPersistentDataContainer().remove(playerAuraPdcKey);
        removeAuraDisplayOnly(player);
    }

    public void handlePlayerQuit(Player player) {
        UUID uuid = player.getUniqueId();
        clearTempTask(uuid);
        hijackedAuraBackups.remove(uuid);
        auraVisibilityDisabled.remove(uuid);
        stopAnimation(uuid);
        removeAuraDisplayOnly(player);
    }

    public void reapplyStoredAura(Player player) {
        if (temporaryTasks.containsKey(player.getUniqueId())) return;

        if (player.isInsideVehicle() 
                || player.isSleeping() 
                || player.getGameMode() == GameMode.SPECTATOR 
                || player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            return;
        }

        String savedId = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
        if (savedId == null) return;

        AuraConfig config = plugin.getAuraConfigs().get(savedId.toLowerCase());
        if (config != null && player.hasPermission(config.getPermission())) {
            spawnAuraDisplay(player, config);
        }
    }

    public void hideAuraForInvisibility(Player player) {
        removeAuraDisplayOnly(player);
    }

    private void spawnAuraDisplay(Player player, AuraConfig config) {
        removeAuraDisplayOnly(player);

        UUID uuid = player.getUniqueId();
        List<NamespacedKey> frames = config.getFrames();
        if (frames.isEmpty()) return;

        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setPitch(0);

        frameIndices.put(uuid, 0);
        NamespacedKey initialModel = frames.get(0);

        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && initialModel != null) {
            meta.setItemModel(initialModel);
            item.setItemMeta(meta);
        }

        ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            entity.setBillboard(config.getBillboard());
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setShadowRadius(0.0f);

            Transformation transform = new Transformation(
                    new Vector3f(config.getOffsetX(), config.getOffsetY(), config.getOffsetZ()),
                    new AxisAngle4f(0, 0, 1, 0),
                    new Vector3f(config.getScale(), config.getScale(), config.getScale()),
                    new AxisAngle4f(0, 0, 1, 0)
            );
            entity.setTransformation(transform);
        });

        player.addPassenger(display);
        activeAuras.put(uuid, display);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (auraVisibilityDisabled.contains(online.getUniqueId())) {
                online.hideEntity(plugin, display);
            }
        }

        if (frames.size() > 1) {
            BukkitTask animTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!player.isOnline() || !display.isValid()) {
                    stopAnimation(uuid);
                    return;
                }

                int currentIndex = frameIndices.getOrDefault(uuid, 0);
                currentIndex = (currentIndex + 1) % frames.size();
                frameIndices.put(uuid, currentIndex);

                NamespacedKey nextModel = frames.get(currentIndex);
                ItemStack stack = display.getItemStack();
                if (stack != null) {
                    ItemMeta im = stack.getItemMeta();
                    if (im != null) {
                        im.setItemModel(nextModel);
                        stack.setItemMeta(im);
                        display.setItemStack(stack);
                    }
                }
            }, config.getFrameDelay(), config.getFrameDelay());

            animationTasks.put(uuid, animTask);
        }
    }

    private void stopAnimation(UUID uuid) {
        BukkitTask task = animationTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        frameIndices.remove(uuid);
    }

    public void removeAuraDisplayOnly(Player player) {
        UUID uuid = player.getUniqueId();
        stopAnimation(uuid);

        ItemDisplay display = activeAuras.remove(uuid);
        if (display != null && display.isValid()) {
            player.removePassenger(display);
            display.remove();
        }
    }
}
