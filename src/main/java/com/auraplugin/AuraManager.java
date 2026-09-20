package com.auraplugin;

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
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuraManager {

    private final AuraPlugin plugin;
    private final NamespacedKey playerAuraPdcKey;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();

    public AuraManager(AuraPlugin plugin) {
        this.plugin = plugin;
        this.playerAuraPdcKey = new NamespacedKey(plugin, "selected_aura_id");
    }

    public void setAura(Player player, AuraConfig config) {
        player.getPersistentDataContainer().set(playerAuraPdcKey, PersistentDataType.STRING, config.getId());
        spawnAuraDisplay(player, config);
    }

    public void removeAura(Player player) {
        player.getPersistentDataContainer().remove(playerAuraPdcKey);
        removeAuraDisplayOnly(player);
    }

    public void reapplyStoredAura(Player player) {
        // Halt spawning if player is in an incompatible state
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

        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setPitch(0); // Flatten pitch angle prior to mounting

        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && config.getItemModel() != null) {
            meta.setItemModel(config.getItemModel());
            item.setItemMeta(meta);
        }

        ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            entity.setBillboard(Display.Billboard.VERTICAL);
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
        activeAuras.put(player.getUniqueId(), display);
    }

    public void removeAuraDisplayOnly(Player player) {
        ItemDisplay display = activeAuras.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            player.removePassenger(display);
            display.remove();
        }
    }
}
