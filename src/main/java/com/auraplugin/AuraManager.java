package com.auraplugin;

import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuraManager {
    private final NamespacedKey auraKey;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();

    public AuraManager(AuraPlugin plugin) {
        this.auraKey = new NamespacedKey(plugin, "player_aura_id");
    }

    public void applyAura(Player player, AuraConfig config) {
        removeAura(player);

        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && config.getItemModel() != null) {
            // New 1.21 Data Component method replacing CustomModelData
            meta.setItemModel(config.getItemModel());
            item.setItemMeta(meta);
        }

        ItemDisplay display = player.getWorld().spawn(player.getLocation(), ItemDisplay.class, entity -> {
            entity.setItemStack(item);
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            entity.setBillboard(config.getBillboard());
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setShadowRadius(0.0f);

            Transformation transform = new Transformation(
                    new Vector3f(config.getOffsetX(), config.getOffsetY(), config.getOffsetZ()),
                    new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f),
                    new Vector3f(config.getScale(), config.getScale(), config.getScale()),
                    new AxisAngle4f(0.0f, 0.0f, 1.0f, 0.0f)
            );
            entity.setTransformation(transform);
            entity.setTeleportDuration(1); // Keeps interpolation ultra-smooth
        });

        // Passenger system prevents needing to teleport the entity on movement loops
        player.addPassenger(display);
        display.getPersistentDataContainer().set(auraKey, PersistentDataType.STRING, player.getUniqueId().toString());
        activeAuras.put(player.getUniqueId(), display);
    }

    public void removeAura(Player player) {
        ItemDisplay display = activeAuras.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            player.removePassenger(display);
            display.remove();
        }
    }
}
