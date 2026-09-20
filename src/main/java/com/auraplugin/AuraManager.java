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

    private final AuraPlugin plugin;
    private final NamespacedKey playerAuraPdcKey;
    private final NamespacedKey entityTagKey;
    private final Map<UUID, ItemDisplay> activeAuras = new HashMap<>();

    public AuraManager(AuraPlugin plugin) {
        this.plugin = plugin;
        // Key saved on the PLAYER to remember their selected cosmetic
        this.playerAuraPdcKey = new NamespacedKey(plugin, "selected_aura_id");
        // Key saved on the ENTITY for cleanup tracking
        this.entityTagKey = new NamespacedKey(plugin, "aura_display_entity");
    }

    /**
     * User equips a new aura cosmetic. Saves selection to Player PDC.
     */
    public void setAura(Player player, AuraConfig config) {
        // Save cosmetic selection persistently to player PDC
        player.getPersistentDataContainer().set(playerAuraPdcKey, PersistentDataType.STRING, config.getId());
        spawnAuraDisplay(player, config);
    }

    /**
     * User explicitly removes their aura via command. Clears selection from Player PDC.
     */
    public void removeAura(Player player) {
        player.getPersistentDataContainer().remove(playerAuraPdcKey);
        removeAuraDisplayOnly(player);
    }

    /**
     * Re-applies the saved aura from PDC if present (used for join/respawn/world change).
     */
    public void reapplyStoredAura(Player player) {
        String savedAuraId = player.getPersistentDataContainer().get(playerAuraPdcKey, PersistentDataType.STRING);
        if (savedAuraId == null) return;

        AuraConfig config = plugin.getAuraConfigs().get(savedAuraId.toLowerCase());
        if (config != null && player.hasPermission(config.getPermission())) {
            spawnAuraDisplay(player, config);
        }
    }

    /**
     * Internal helper to spawn and mount the entity without altering saved player data.
     */
    private void spawnAuraDisplay(Player player, AuraConfig config) {
        removeAuraDisplayOnly(player); // Clean up existing entity if present

        ItemStack item = new ItemStack(config.getMaterial());
        ItemMeta meta = item.getItemMeta();
        if (meta != null && config.getItemModel() != null) {
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
            entity.setTeleportDuration(1);
        });

        player.addPassenger(display);
        display.getPersistentDataContainer().set(entityTagKey, PersistentDataType.STRING, player.getUniqueId().toString());
        activeAuras.put(player.getUniqueId(), display);
    }

    /**
     * Removes only the display entity (on death, quit, world change) while preserving player selection in PDC.
     */
    public void removeAuraDisplayOnly(Player player) {
        ItemDisplay display = activeAuras.remove(player.getUniqueId());
        if (display != null && display.isValid()) {
            player.removePassenger(display);
            display.remove();
        }
    }
}
