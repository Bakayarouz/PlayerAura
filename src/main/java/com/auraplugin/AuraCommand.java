package com.auraplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AuraCommand implements CommandExecutor, TabCompleter {

    private final AuraPlugin plugin;

    public AuraCommand(AuraPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                              @NotNull String label, @NotNull String[] args) {
        MessageManager msg = plugin.getMessageManager();

        if (args.length < 1) {
            msg.send(sender, "commands.usage");
            return true;
        }

        String subAction = args[0].toLowerCase();

        if (subAction.equals("toggle")) {
            if (!(sender instanceof Player)) {
                msg.send(sender, "commands.player-only");
                return true;
            }
            Player player = (Player) sender;
            String toggleOption = args.length > 1 ? args[1].toLowerCase() : "all";

            if (toggleOption.equals("self")) {
                boolean hidden = plugin.getAuraManager().toggleSelf(player);
                msg.send(player, hidden ? "toggle.self-hidden" : "toggle.self-visible");
            } else if (toggleOption.equals("others") || toggleOption.equals("other")) {
                boolean hidden = plugin.getAuraManager().toggleOthers(player);
                msg.send(player, hidden ? "toggle.others-hidden" : "toggle.others-visible");
            } else if (toggleOption.equals("all")) {
                boolean hidden = plugin.getAuraManager().toggleAll(player);
                msg.send(player, hidden ? "toggle.all-hidden" : "toggle.all-visible");
            } else {
                msg.send(sender, "commands.usage");
            }
            return true;
        }

        if (!sender.hasPermission("aura.admin")) {
            msg.send(sender, "commands.no-permission");
            return true;
        }

        if (subAction.equals("reload")) {
            plugin.reloadAuraConfig();
            msg.send(sender, "commands.reloaded");
            return true;
        }

        if (subAction.equals("set") || subAction.equals("remove") || subAction.equals("temp")) {
            if (args.length < 2) {
                msg.send(sender, "commands.usage");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                msg.send(sender, "commands.player-not-found");
                return true;
            }

            HashMap<String, String> placeholders = new HashMap<>();
            placeholders.put("target", target.getName());

            if (subAction.equals("remove")) {
                plugin.getAuraManager().removeAura(target);
                msg.send(sender, "actions.aura-removed-sender", placeholders);
                return true;
            }

            if (subAction.equals("set")) {
                if (args.length < 3) {
                    msg.send(sender, "commands.usage");
                    return true;
                }
                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);
                if (config == null) {
                    placeholders.put("auras", String.join(", ", plugin.getAuraConfigs().keySet()));
                    msg.send(sender, "commands.invalid-aura", placeholders);
                    return true;
                }
                placeholders.put("aura", config.getId());
                plugin.getAuraManager().setAura(target, config);
                msg.send(sender, "actions.aura-set-sender", placeholders);
                return true;
            }

            if (subAction.equals("temp")) {
                if (args.length < 4) {
                    msg.send(sender, "commands.usage");
                    return true;
                }
                String auraId = args[2].toLowerCase();
                AuraConfig config = plugin.getAuraConfigs().get(auraId);
                if (config == null) {
                    msg.send(sender, "commands.invalid-aura");
                    return true;
                }

                int seconds;
                try {
                    seconds = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    msg.send(sender, "commands.invalid-duration");
                    return true;
                }

                placeholders.put("aura", config.getId());
                placeholders.put("seconds", String.valueOf(seconds));
                plugin.getAuraManager().setTemporaryAura(target, config, seconds);
                msg.send(sender, "actions.aura-temp-sender", placeholders);
                return true;
            }
        }

        msg.send(sender, "commands.usage");
        return true;
    }

    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        ArrayList<String> completions = new ArrayList<>();

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
            } else if (sender.hasPermission("aura.admin")
                    && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("temp"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (sender.hasPermission("aura.admin") && args.length == 3
                && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("temp"))) {
            completions.addAll(plugin.getAuraConfigs().keySet());
        }

        String currentArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(currentArg))
                .collect(Collectors.toList());
    }
}
