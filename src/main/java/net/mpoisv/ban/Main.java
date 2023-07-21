package net.mpoisv.ban;

import net.mpoisv.ban.command.Uuid;
import net.mpoisv.ban.utils.UUIDConverter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main extends JavaPlugin {
    public static Main instance;
    public ThreadManager threadManager;
    public DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        instance = this;
        getDataFolder().mkdirs();

        threadManager = new ThreadManager();
        try {
            databaseManager = new DatabaseManager(getDataFolder().getAbsolutePath(), "player.db");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        initCommand(Objects.requireNonNull(getCommand("uuid")), new Uuid());
        Bukkit.getPluginManager().registerEvents(new EventManager(), this);

        threadManager.startWorker();
        Bukkit.getConsoleSender().sendMessage("§bː§f UUID §bː §rPlugin Loading finished. Current version: " + getDescription().getVersion());
    }

    private void initCommand(PluginCommand command, Object instance) {
        command.setExecutor((CommandExecutor) instance);
        command.setTabCompleter((TabCompleter) instance);
    }

    @Override
    public void onDisable() {
        instance = null;
        threadManager.stop();
        try {
            databaseManager.close();
        }catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String color(String str) {
        return str.replaceAll("&0", "§0")
                .replaceAll("&1", "§1")
                .replaceAll("&2", "§2")
                .replaceAll("&3", "§3")
                .replaceAll("&4", "§4")
                .replaceAll("&5", "§5")
                .replaceAll("&6", "§6")
                .replaceAll("&7", "§7")
                .replaceAll("&8", "§8")
                .replaceAll("&9", "§9")
                .replaceAll("&a", "§a")
                .replaceAll("&b", "§b")
                .replaceAll("&c", "§c")
                .replaceAll("&d", "§d")
                .replaceAll("&e", "§e")
                .replaceAll("&f", "§f")
                .replaceAll("&k", "§k")
                .replaceAll("&l", "§l")
                .replaceAll("&m", "§m")
                .replaceAll("&n", "§n")
                .replaceAll("&o", "§o");
    }

    public static String getCalendarString(LocalDateTime date) {
        return date.getYear() + "§aY §f"
                + date.getMonthValue() + "§aM §f"
                + date.getDayOfMonth() + "§aD §f"
                + date.getHour() + "§aH §f"
                + date.getMinute() + "§aM§f";
    }

    public static String getCalendarString(LocalDateTime a, LocalDateTime b) {
        var duration = Duration.between(a, b).abs();
        var period = Period.between(a.toLocalDate(), b.toLocalDate());
        return period.getYears() + "§aY §f"
                + period.getMonths() + "§aM §f"
                + period.getDays() + "§aD §f"
                + duration.toHoursPart() + "§aH §f"
                + duration.toMinutesPart() + "§aM§f";
    }

    public static Inventory getListInventory(DatabaseManager.Pagination pagination, String title) {
        var inv = Bukkit.createInventory(null, 54, title);
        var currentMillis = System.currentTimeMillis();
        var now = Instant.ofEpochMilli(currentMillis).atZone(ZoneOffset.UTC).toLocalDateTime();

        for(var data : pagination.getValues()) {
            var uuid = UUIDConverter.getUUIDFromUUID(data.getUuid());
            var player = Bukkit.getOfflinePlayer(uuid);
            var head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skull = (SkullMeta) head.getItemMeta();
            Objects.requireNonNull(skull).setOwningPlayer(player);
            skull.setDisplayName("§a" + (player.getName() == null ? "알수없는 사용자" : player.getName()));
            if(data.getTime() > 0) {
                var pardonTime = Instant.ofEpochMilli(data.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
                var pardonText = Main.getCalendarString(pardonTime);
                var periodText = Main.getCalendarString(now, pardonTime);
                skull.setLore(Arrays.asList("", "§cReason: §f" + data.getReason(),
                        "§cUntil: §f§n" + pardonText,
                        "§cRemain Days: §f§n" + periodText,
                        "§fUUID: §7" + data.getUuid()));
            }else
                skull.setLore(Arrays.asList("", "§cReason: §f" + data.getReason(),
                        "§cUntil: §f§nInfinity",
                        "§fUUID: §7" + data.getUuid()));
            head.setItemMeta(skull);
            inv.addItem(head);
        }

        var lore = List.of("§ePage: " + pagination.getCurrentPage() + "/" + pagination.getMaxPage());
        var item = new ItemStack(Material.PAPER);
        var meta = item.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName("§bBefore Page");
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(52, item);

        item = new ItemStack(Material.PAPER);
        meta = item.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName("§bNext Page");
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(53, item);

        return inv;
    }
}