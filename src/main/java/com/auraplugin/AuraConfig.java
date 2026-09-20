package com.auraplugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display.Billboard;

public class AuraConfig {
    private final String id;
    private final String displayName;
    private final Material material;
    private final NamespacedKey itemModel;
    private final String permission;
    private final float offsetY, offsetX, offsetZ, scale;
    private final Billboard billboard;

    public AuraConfig(String id, ConfigurationSection section) {
        this.id = id;
        this.displayName = section.getString("display-name", id);
        this.material = Material.matchMaterial(section.getString("material", "PAPER"));
        
        // Load the 1.21 item_model namespace (e.g. "my_pack:fire_ring")
        String rawModel = section.getString("item-model", "minecraft:paper");
        this.itemModel = NamespacedKey.fromString(rawModel);

        this.permission = section.getString("permission", "aura.use." + id);
        this.offsetY = (float) section.getDouble("offset-y", 0.0);
        this.offsetX = (float) section.getDouble("offset-x", 0.0);
        this.offsetZ = (float) section.getDouble("offset-z", 0.0);
        this.scale = (float) section.getDouble("scale", 1.0);
        
        Billboard parsed;
        try {
            parsed = Billboard.valueOf(section.getString("billboard", "CENTER").toUpperCase());
        } catch (IllegalArgumentException e) {
            parsed = Billboard.CENTER;
        }
        this.billboard = parsed;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Material getMaterial() { return material != null ? material : Material.PAPER; }
    public NamespacedKey getItemModel() { return itemModel; }
    public String getPermission() { return permission; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetZ() { return offsetZ; }
    public float getScale() { return scale; }
    public Billboard getBillboard() { return billboard; }
}
