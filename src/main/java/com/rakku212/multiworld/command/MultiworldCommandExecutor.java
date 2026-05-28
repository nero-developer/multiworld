package com.rakku212.multiworld.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.rakku212.multiworld.internal.service.MultiworldService;
import com.rakku212.multiworld.internal.util.DimensionParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class MultiworldCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String PERM_CREATE = "multiworld.create";
    private static final String PERM_LIST = "multiworld.list";
    private static final String PERM_REMOVE = "multiworld.remove";
    private static final String PERM_TP = "multiworld.tp";

    private final JavaPlugin plugin;
    private final MultiworldService service;

    public MultiworldCommandExecutor(JavaPlugin plugin, MultiworldService service) {
        this.plugin = plugin;
        this.service = service;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> handleCreate(sender, args);
            case "list" -> handleList(sender);
            case "remove" -> handleRemove(sender, args);
            case "tp" -> handleTp(sender, args);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_CREATE)) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "使い方: /multiworld create <ディメンション> <ワールド名> [シード]",
                    NamedTextColor.YELLOW));
            return;
        }
        String dimRaw = args[1];
        String worldName = args[2];
        OptionalLong seed = OptionalLong.empty();
        if (args.length >= 4) {
            try {
                seed = OptionalLong.of(Long.parseLong(args[3]));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("シードは整数で指定してください。", NamedTextColor.RED));
                return;
            }
        }

        var envOpt = DimensionParser.parse(dimRaw);
        if (envOpt.isEmpty()) {
            sender.sendMessage(Component.text(
                    "ディメンションが不正です (overworld / nether / end など)。",
                    NamedTextColor.RED));
            return;
        }
        World.Environment env = envOpt.get();

        sender.sendMessage(Component.text("ワールド作成を準備しています…", NamedTextColor.GRAY));
        service.createManagedWorldAsync(env, worldName, seed).whenComplete((world, error) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (error != null) {
                    Throwable c = unwrap(error);
                    sender.sendMessage(Component.text(
                            "作成に失敗しました: " + c.getMessage(),
                            NamedTextColor.RED));
                    return;
                }
                sender.sendMessage(Component.text(
                        "ワールド \"" + world.getName() + "\" を作成しました。(seed=" + world.getSeed() + ")",
                        NamedTextColor.GREEN));
            });
        });
    }

    private static Throwable unwrap(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null && c != c.getCause()) {
            c = c.getCause();
        }
        return c;
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission(PERM_LIST)) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        var list = service.listManagedWorlds();
        if (list.isEmpty()) {
            sender.sendMessage(Component.text("登録された管理ワールドはありません。", NamedTextColor.GRAY));
            return;
        }
        sender.sendMessage(Component.text("管理ワールド一覧:", NamedTextColor.GOLD));
        for (var info : list) {
            String loaded = info.loaded() ? "ロード済" : "未ロード";
            sender.sendMessage(Component.text(
                    "- " + info.name()
                            + " | " + info.environment().name()
                            + " | seed=" + info.seed()
                            + " | " + loaded,
                    NamedTextColor.WHITE));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_REMOVE)) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("使い方: /multiworld remove <ワールド名>", NamedTextColor.YELLOW));
            return;
        }
        String worldName = args[1];
        if (!service.isManaged(worldName)) {
            sender.sendMessage(Component.text(
                    "そのワールドはプラグイン管理下にありません (データベースに未登録)。",
                    NamedTextColor.RED));
            return;
        }
        boolean ok = service.removeManagedWorld(worldName);
        if (ok) {
            sender.sendMessage(Component.text(
                    "ワールド \"" + worldName + "\" をアンロードし、登録から削除しました。",
                    NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("削除に失敗しました。", NamedTextColor.RED));
        }
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!sender.hasPermission(PERM_TP)) {
            sender.sendMessage(Component.text("権限がありません。", NamedTextColor.RED));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("このコマンドはプレイヤーのみ実行できます。", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("使い方: /multiworld tp <ワールド名>", NamedTextColor.YELLOW));
            return;
        }
        String worldName = args[1];
        if (!service.teleportPlayer(player, worldName)) {
            sender.sendMessage(Component.text(
                    "ワールド \"" + worldName + "\" が見つからないか、読み込まれていません。",
                    NamedTextColor.RED));
            return;
        }
        sender.sendMessage(Component.text(
                "ワールド \"" + player.getWorld().getName() + "\" にテレポートしました。",
                NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "使い方: /multiworld <create|list|remove|tp> ...",
                NamedTextColor.YELLOW));
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return partialMatch(List.of("create", "list", "remove", "tp"), args[0]);
        }
        if (args.length == 2 && "create".equalsIgnoreCase(args[0])) {
            return partialMatch(List.of("overworld", "nether", "end"), args[1]);
        }
        if (args.length == 3 && "create".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }
        if (args.length == 4 && "create".equalsIgnoreCase(args[0])) {
            return Collections.emptyList();
        }
        if (args.length == 2 && "remove".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(PERM_REMOVE)) {
                return Collections.emptyList();
            }
            return partialMatch(
                    service.listManagedWorlds().stream().map(i -> i.name()).collect(Collectors.toList()),
                    args[1]);
        }
        if (args.length == 2 && "tp".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(PERM_TP)) {
                return Collections.emptyList();
            }
            return partialMatch(
                    Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList()),
                    args[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> partialMatch(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(p)) {
                out.add(o);
            }
        }
        return out;
    }
}
