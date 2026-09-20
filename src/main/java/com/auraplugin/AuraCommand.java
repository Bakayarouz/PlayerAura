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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "Usage: /aura <set|remove|list|reload> [aura_id]");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "remove", "off" -> {
                // Clears Player PDC selection and removes display entity
                plugin.getAuraManager().removeAura(player);
                player.sendMessage(ChatColor.RED + "Aura cosmetic removed.");
                return true;
            }

            case "reload" -> {
                if (!player.hasPermission("aura.admin")) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to reload the configuration.");
                    return true;
                }
                plugin.reloadAuraConfig();
                player.sendMessage(ChatColor.GREEN + "Aura configuration reloaded successfully!");
                return true;
            }

            case "list" -> {
                player.sendMessage(ChatColor.GOLD + "=== Available Auras ===");
                boolean foundAny = false;
                for (AuraConfig cfg : plugin.getAuraConfigs().values()) {
                    if (player.hasPermission(cfg.getPermission())) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                " - " + cfg.getDisplayName() + " &7(" + cfg.getId() + ")"));
                        foundAny = true;
                    }
                }
                if (!foundAny) {
                    player.sendMessage(ChatColor.GRAY + "You do not have permission for any available auras.");
                }
                return true;
            }

            case "set" -> {
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /aura set <aura_id>");
                    return true;
                }

                String auraId = args[1].toLowerCase();
                AuraConfig cfg = plugin.getAuraConfigs().get(auraId);

                if (cfg == null) {
                    player.sendMessage(ChatColor.RED + "Aura '" + auraId + "' does not exist.");
                    return true;
                }

                if (!player.hasPermission(cfg.getPermission())) {
                    player.sendMessage(ChatColor.RED + "You do not have permission to use this aura!");
                    return true;
                }

                // Saves selection to Player PDC & mounts display entity
                plugin.getAuraManager().setAura(player, cfg);
                player.sendMessage(ChatColor.GREEN + "Equipped cosmetic: " + 
                        ChatColor.translateAlternateColorCodes('&', cfg.getDisplayName()));
                return true;
            }

            default -> {
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /aura <set|remove|list|reload>");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subcommands = new ArrayList<>(List.of("set", "remove", "list"));
            if (sender.hasPermission("aura.admin")) {
                subcommands.add("reload");
            }
            for (String sub : subcommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String input = args[1].toLowerCase();
            for (AuraConfig cfg : plugin.getAuraConfigs().values()) {
                if (sender.hasPermission(cfg.getPermission()) && cfg.getId().startsWith(input)) {
                    completions.add(cfg.getId());
                }
            }
        }

        return completions;
    }
}
