package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuraCommand implements CommandExecutor, TabCompleter {

    private final AuraPlugin plugin;

    public AuraCommand(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("§cUsage: /aura <set|remove|reload> [player] [aura_id]");
            return true;
        }

        String subAction = args[0].toLowerCase();

        // Handle reload (Admin only)
        if (subAction.equals("reload")) {
            if (!sender.hasPermission("aura.admin")) {
                sender.sendMessage("§cYou do not have permission to reload auras.");
                return true;
            }
            plugin.reloadAuraConfig();
            sender.sendMessage("§aAura configuration reloaded successfully!");
            return true;
        }

        // Handle /aura set <player> <id> or /aura remove <player>
        if (subAction.equals("set") || subAction.equals("remove")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /aura " + subAction + " <player> [aura_id]");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            if (subAction.equals("remove")) {
                plugin.getAuraManager().removeAura(target);
                sender.sendMessage("§aRemoved aura from " + target.getName());
                if (sender != target) {
                    target.sendMessage("§eYour aura has been removed.");
                }
                return true;
            }

            if (subAction.equals("set")) {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /aura set <player> <aura_id>");
                    return true;
                }

                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);

                if (config == null) {
                    sender.sendMessage("§cInvalid aura ID! Available: " + String.join(", ", plugin.getAuraConfigs().keySet()));
                    return true;
                }

                // Check permission: If executed by console or admin, check if target player has permission (or bypass)
                boolean bypass = sender.hasPermission("aura.admin") || sender.equals(target);
                if (!bypass && !target.hasPermission(config.getPermission())) {
                    sender.sendMessage("§cThat player does not have permission to use this aura.");
                    return true;
                }

                plugin.getAuraManager().setAura(target, config);
                sender.sendMessage("§aSuccessfully applied aura '" + config.getId() + "' to " + target.getName());
                if (sender != target) {
                    target.sendMessage("§aYou have been equipped with the '" + config.getId() + "' aura!");
                }
                return true;
            }
        }

        sender.sendMessage("§cUnknown subcommand. Use /aura set, remove, or reload.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("set");
            completions.add("remove");
            if (sender.hasPermission("aura.admin")) {
                completions.add("reload");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove"))) {
            // Suggest online player names
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            // Suggest available aura IDs from config
            completions.addAll(plugin.getAuraConfigs().keySet());
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
