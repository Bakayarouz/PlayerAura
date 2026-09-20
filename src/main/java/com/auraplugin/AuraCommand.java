package com.auraplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuraCommand implements CommandExecutor, TabCompleter {

    private final AuraPlugin plugin;

    public AuraCommand(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 1) {
            sender.sendMessage(msg.get("commands.usage"));
            return true;
        }

        String subAction = args[0].toLowerCase();

        if (subAction.equals("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(msg.get("commands.player-only"));
                return true;
            }

            String toggleOption = args.length > 1 ? args[1].toLowerCase() : "all";

            if (toggleOption.equals("self")) {
                boolean hidden = plugin.getAuraManager().toggleSelf(player);
                player.sendMessage(hidden ? msg.get("toggle.self-hidden") : msg.get("toggle.self-visible"));
            } else if (toggleOption.equals("others") || toggleOption.equals("other")) {
                boolean hidden = plugin.getAuraManager().toggleOthers(player);
                player.sendMessage(hidden ? msg.get("toggle.others-hidden") : msg.get("toggle.others-visible"));
            } else if (toggleOption.equals("all")) {
                boolean hidden = plugin.getAuraManager().toggleAll(player);
                player.sendMessage(hidden ? msg.get("toggle.all-hidden") : msg.get("toggle.all-visible"));
            } else {
                sender.sendMessage(msg.get("commands.usage"));
            }
            return true;
        }

        if (!sender.hasPermission("aura.admin")) {
            sender.sendMessage(msg.get("commands.no-permission"));
            return true;
        }

        if (subAction.equals("reload")) {
            plugin.reloadAuraConfig();
            sender.sendMessage(msg.get("commands.reloaded"));
            return true;
        }

        if (subAction.equals("set") || subAction.equals("remove") || subAction.equals("temp")) {
            if (args.length < 2) {
                sender.sendMessage(msg.get("commands.usage"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage(msg.get("commands.player-not-found"));
                return true;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("target", target.getName());

            if (subAction.equals("remove")) {
                plugin.getAuraManager().removeAura(target);
                sender.sendMessage(msg.get("actions.aura-removed-sender", placeholders));
                return true;
            }

            if (subAction.equals("set")) {
                if (args.length < 3) {
                    sender.sendMessage(msg.get("commands.usage"));
                    return true;
                }

                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);

                if (config == null) {
                    placeholders.put("auras", String.join(", ", plugin.getAuraConfigs().keySet()));
                    sender.sendMessage(msg.get("commands.invalid-aura", placeholders));
                    return true;
                }

                placeholders.put("aura", config.getId());
                plugin.getAuraManager().setAura(target, config);
                sender.sendMessage(msg.get("actions.aura-set-sender", placeholders));
                return true;
            }

            if (subAction.equals("temp")) {
                if (args.length < 4) {
                    sender.sendMessage(msg.get("commands.usage"));
                    return true;
                }

                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);
                if (config == null) {
                    sender.sendMessage(msg.get("commands.invalid-aura"));
                    return true;
                }

                int seconds;
                try {
                    seconds = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(msg.get("commands.invalid-duration"));
                    return true;
                }

                placeholders.put("aura", config.getId());
                placeholders.put("seconds", String.valueOf(seconds));

                plugin.getAuraManager().setTemporaryAura(target, config, seconds);
                sender.sendMessage(msg.get("actions.aura-temp-sender", placeholders));
                return true;
            }
        }

        sender.sendMessage(msg.get("commands.usage"));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("toggle");
            if (sender.hasPermission("aura.admin")) {
                completions.add("set");
                completions.add("remove");
                completions.add("temp");
                completions.add("reload");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("toggle")) {
                completions.add("self");
                completions.add("others");
                completions.add("all");
            } else if (sender.hasPermission("aura.admin") && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("temp"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (sender.hasPermission("aura.admin") && args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("temp"))) {
            completions.addAll(plugin.getAuraConfigs().keySet());
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
