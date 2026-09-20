package com.auraplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;

import java.time.Duration;
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
    
    private final AuraAction whenApplied;
    private final AuraAction whenDisabled;

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

        this.whenApplied = new AuraAction(section.getConfigurationSection("when-applied"));
        this.whenDisabled = new AuraAction(section.getConfigurationSection("when-disabled"));
    }

    public static class AuraAction {
        private final String message;
        private final String title;
        private final String subtitle;
        private final String actionbar;

        public AuraAction(ConfigurationSection section) {
            if (section == null) {
                this.message = null;
                this.title = null;
                this.subtitle = null;
                this.actionbar = null;
            } else {
                this.message = section.getString("message", null);
                this.title = section.getString("title", null);
                this.subtitle = section.getString("subtitle", null);
                this.actionbar = section.getString("actionbar", null);
            }
        }

        public void execute(Player player) {
            LegacyComponentSerializer serializer = LegacyComponentSerializer.legacy('&');

            if (message != null && !message.isEmpty()) {
                player.sendMessage(serializer.deserialize(message));
            }

            if (actionbar != null && !actionbar.isEmpty()) {
                player.sendActionBar(serializer.deserialize(actionbar));
            }

            if ((title != null && !title.isEmpty()) || (subtitle != null && !subtitle.isEmpty())) {
                Component titleComp = title != null ? serializer.deserialize(title) : Component.empty();
                Component subtitleComp = subtitle != null ? serializer.deserialize(subtitle) : Component.empty();
                
                Title.Times times = Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(500));
                player.showTitle(Title.title(titleComp, subtitleComp, times));
            }
        }
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
    public AuraAction getWhenApplied() { return whenApplied; }
    public AuraAction getWhenDisabled() { return whenDisabled; }
}
