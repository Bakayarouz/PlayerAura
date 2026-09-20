package com.auraplugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AuraCommand implements CommandExecutor, TabCompleter {
    private final AuraPlugin plugin;

    public AuraCommand(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /aura <set|remove|list|reload> [id]");
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("remove") || sub.equals("off")) {
            plugin.getAuraManager().removeAura(player);
            player.sendMessage(ChatColor.RED + "Aura removed.");
        } else if (sub.equals("reload") && player.hasPermission("aura.admin")) {
            plugin.reloadAuraConfig();
            player.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
        } else if (sub.equals("list")) {
            player.sendMessage(ChatColor.GOLD + "Available Auras:");
            plugin.getAuraConfigs().values().stream()
                    .filter(cfg -> player.hasPermission(cfg.getPermission()))
                    .forEach(cfg -> player.sendMessage(ChatColor.translateAlternateColorCodes('&', " - " + cfg.getDisplayName() + " &7(" + cfg.getId() + ")")));
        } else if (sub.equals("set") && args.length > 1) {
            AuraConfig cfg = plugin.getAuraConfigs().get(args[1].toLowerCase());
            if (cfg == null || !player.hasPermission(cfg.getPermission())) {
                player.sendMessage(ChatColor.RED + "Invalid aura or missing permissions.");
                return true;
            }
            plugin.getAuraManager().applyAura(player, cfg);
            player.sendMessage(ChatColor.GREEN + "Aura applied.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("set", "remove", "list"));
            if (sender.hasPermission("aura.admin")) completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            plugin.getAuraConfigs().values().forEach(cfg -> {
                if (sender.hasPermission(cfg.getPermission())) completions.add(cfg.getId());
            });
        }
        return completions;
    }
}
