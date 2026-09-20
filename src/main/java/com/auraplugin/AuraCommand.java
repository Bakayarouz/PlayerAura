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
            sender.sendMessage("§cUsage: /aura <set|remove|temp|toggle|reload> ...");
            return true;
        }

        String subAction = args[0].toLowerCase();

        if (subAction.equals("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can toggle aura visibility.");
                return true;
            }
            boolean visible = plugin.getAuraManager().toggleAuraVisibility(player);
            if (visible) {
                player.sendMessage("§aAuras are now §evisible §aaround other players.");
            } else {
                player.sendMessage("§eAuras are now §chidden §efrom your view.");
            }
            return true;
        }

        if (subAction.equals("reload")) {
            if (!sender.hasPermission("aura.admin")) {
                sender.sendMessage("§cYou do not have permission to reload auras.");
                return true;
            }
            plugin.reloadAuraConfig();
            sender.sendMessage("§aAura configuration reloaded successfully!");
            return true;
        }

        if (subAction.equals("set") || subAction.equals("remove") || subAction.equals("temp")) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /aura " + subAction + " <player> [aura_id] [seconds]");
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

            if (subAction.equals("temp")) {
                if (!sender.hasPermission("aura.admin")) {
                    sender.sendMessage("§cYou do not have permission to give temporary auras.");
                    return true;
                }

                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /aura temp <player> <aura_id> <seconds>");
                    return true;
                }

                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);
                if (config == null) {
                    sender.sendMessage("§cInvalid aura ID!");
                    return true;
                }

                int seconds;
                try {
                    seconds = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cDuration must be a valid number of seconds.");
                    return true;
                }

                plugin.getAuraManager().setTemporaryAura(target, config, seconds);
                sender.sendMessage("§aApplied temporary aura '" + config.getId() + "' to " + target.getName() + " for " + seconds + "s.");
                target.sendMessage("§eYou received a temporary aura (" + config.getId() + ") for " + seconds + " seconds!");
                return true;
            }
        }

        sender.sendMessage("§cUnknown subcommand. Use /aura set, remove, temp, toggle, or reload.");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("set");
            completions.add("remove");
            completions.add("temp");
            completions.add("toggle");
            if (sender.hasPermission("aura.admin")) {
                completions.add("reload");
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("temp"))) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                completions.add(p.getName());
            }
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("temp"))) {
            completions.addAll(plugin.getAuraConfigs().keySet());
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
