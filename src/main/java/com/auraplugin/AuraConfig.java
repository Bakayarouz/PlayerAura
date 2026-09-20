package com.auraplugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;

import java.util.ArrayList;
import java.util.List;

public class AuraConfig {
    private final String id;
    private final Material material;
    private final List<NamespacedKey> frames;
    private final int frameDelay;
    private final String permission;
    private final float scale;
    private final float offsetX, offsetY, offsetZ;
    private final Display.Billboard billboard;

    public AuraConfig(String id, ConfigurationSection section) {
        this.id = id.toLowerCase();
        this.material = Material.matchMaterial(section.getString("material", "PAPER"));
        
        this.frames = new ArrayList<>();
        if (section.isList("frames")) {
            for (String modelStr : section.getStringList("frames")) {
                NamespacedKey key = NamespacedKey.fromString(modelStr);
                if (key != null) frames.add(key);
            }
        }
        
        // Fallback to single item-model if frames list is empty/missing
        if (frames.isEmpty()) {
            String modelStr = section.getString("item-model", null);
            if (modelStr != null) {
                NamespacedKey key = NamespacedKey.fromString(modelStr);
                if (key != null) frames.add(key);
            }
        }
        
        this.frameDelay = section.getInt("frame-delay", 10);
        this.permission = section.getString("permission", "aura.use." + this.id);
        this.scale = (float) section.getDouble("scale", 1.0);
        this.offsetX = (float) section.getDouble("offset.x", 0.0);
        this.offsetY = (float) section.getDouble("offset.y", 0.0);
        this.offsetZ = (float) section.getDouble("offset.z", 0.0);
        
        String billboardStr = section.getString("billboard", "VERTICAL").toUpperCase();
        Display.Billboard parsedBillboard;
        try {
            parsedBillboard = Display.Billboard.valueOf(billboardStr);
        } catch (IllegalArgumentException e) {
            parsedBillboard = Display.Billboard.VERTICAL;
        }
        this.billboard = parsedBillboard;
    }

    public String getId() { return id; }
    public Material getMaterial() { return material != null ? material : Material.PAPER; }
    public List<NamespacedKey> getFrames() { return frames; }
    public int getFrameDelay() { return frameDelay; }
    public String getPermission() { return permission; }
    public float getScale() { return scale; }
    public float getOffsetX() { return offsetX; }
    public float getOffsetY() { return offsetY; }
    public float getOffsetZ() { return offsetZ; }
    public Display.Billboard getBillboard() { return billboard; }
}
