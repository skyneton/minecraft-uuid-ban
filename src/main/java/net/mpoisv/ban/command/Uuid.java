package net.mpoisv.ban.command;

import net.mpoisv.ban.DatabaseManager;
import net.mpoisv.ban.Main;
import net.mpoisv.ban.utils.UUIDConverter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

public class Uuid implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if(args.length == 0)
            return helpMessage(sender, s);

        switch (args[0].toLowerCase()) {
            case "list" -> {
                return banList(sender, args);
            }
            case "help" -> {
                return helpMessage(sender, s);
            }
        }

        if(args.length <= 2) {
            sender.sendMessage("§bː§f UUID §bː §r명령어를 확인해주세요.");
            return true;
        }
        UUID uuid;
        try {
            uuid = UUIDConverter.getUUID(args[1]);
        }catch (Exception e) {
            sender.sendMessage("§bː§f UUID §bː §rMojang API Server Error. UUID를 가져올 수 없습니다. " + e.getLocalizedMessage() + " : " + e.getCause());
            return true;
        }
        if(uuid == null) {
            sender.sendMessage("§bː§f UUID §bː §r서버로부터 UUID를 가져올 수 없습니다.");
            return true;
        }
        var target = Bukkit.getOfflinePlayer(uuid);
        return switch (args[0].toLowerCase()) {
            case "ban" -> ban(sender, args, uuid.toString(), target);
            case "tban" -> timeBan(sender, args, uuid.toString(), target);
            case "pardon" -> pardon(sender, args, uuid.toString(), target);
            default -> false;
        };
    }

    private boolean helpMessage(CommandSender sender, String label) {
        sender.sendMessage("§bː§f UUID §bː §r/" + label + " tban [player] [year] {month} {day} {hour} {minute} [reason]");
        sender.sendMessage("§bː§f UUID §bː §r/" + label + " ban [player] [reason]");
        sender.sendMessage("§bː§f UUID §bː §r/" + label + " pardon [player]");
        sender.sendMessage("§bː§f UUID §bː §r/" + label + " list {page}");
        sender.sendMessage("");
        sender.sendMessage("§bː§f UUID §bː §r[item] = must usage, {item} can usage.");
        return true;
    }

    private String getReasonFromText(String[] args, int start) {
        var buf = new StringBuffer();
        for(int i = start, end = args.length; i < end; i++) {
            if(i > start) buf.append(" ");
            buf.append(args[i]);
        }
        return Main.color(buf.toString());
    }

    private boolean banList(CommandSender sender, String[] args) {
        int page = 1;
        if(args.length >= 2 && args[1].chars().allMatch(Character::isDigit)) {
            page = Integer.parseInt(args[1]);
        }
        DatabaseManager.Pagination pagination;
        if(args.length >= 2 || !(sender instanceof Player)) {
            try {
                pagination = Main.instance.databaseManager.getPagination(page, 15);
            }catch (Exception e) {
                sender.sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return true;
            }
            sender.sendMessage("§c==============================");
            for(var data : pagination.getValues()) {
                var uuid = UUIDConverter.getUUIDFromUUID(data.getUuid());
                sender.sendMessage(Bukkit.getOfflinePlayer(uuid).getName() + "(" + data.getUuid() + ")");
            }
            sender.sendMessage("§c==============================");
            sender.sendMessage("§e"+pagination.getCurrentPage() + "/" + pagination.getMaxPage() + "§f Page");
            return true;
        }

        try {
            pagination = Main.instance.databaseManager.getPagination(page, 52);
        }catch (Exception e) {
            sender.sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return true;
        }
        ((Player) sender).openInventory(Main.getListInventory(pagination, "§3" + pagination.getCurrentPage() + " Ban List"));
        return true;
    }

    private boolean pardon(CommandSender sender, String[] args, String uuid, OfflinePlayer target) {
        int result;
        try {
            result = Main.instance.databaseManager.delete(uuid);
        }catch(Exception e) {
            sender.sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return true;
        }

        if(result <= 0) {
            sender.sendMessage("§bː§f UUID §bː §r밴된 유저가 아니거나 삭제를 실패했습니다.");
            return true;
        }
        sender.sendMessage("§bː§f UUID §bː §r" + target.getName() + "님이 언밴 되었습니다.");
        return true;
    }

    private boolean timeBan(CommandSender sender, String[] args, String uuid, OfflinePlayer target) {
        if(args.length < 4 || !args[2].chars().allMatch(Character::isDigit)) {
            sender.sendMessage("§bː§f UUID §bː §r명령어를 확인해주세요.");
            return true;
        }

        int year = Integer.parseInt(args[2]), month = 0, day = 0, hour = 0, minute = 0;
        int reasonIndex = 3;
        if(args.length >= 5 && args[3].chars().allMatch(Character::isDigit)) {
            month = Integer.parseInt(args[3]);
            reasonIndex++;
        }
        if(args.length >= 6 && args[4].chars().allMatch(Character::isDigit)) {
            day = Integer.parseInt(args[4]);
            reasonIndex++;
        }
        if(args.length >= 7 && args[5].chars().allMatch(Character::isDigit)) {
            hour = Integer.parseInt(args[5]);
            reasonIndex++;
        }
        if(args.length >= 8 && args[6].chars().allMatch(Character::isDigit)) {
            minute = Integer.parseInt(args[6]);
            reasonIndex++;
        }

        var reason = getReasonFromText(args, reasonIndex);
        var now = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneOffset.UTC).toLocalDateTime();
        var pardonTime = now.plusYears(year).plusMonths(month).plusDays(day).plusHours(hour).plusMinutes(minute);
        var pardonMillis = pardonTime.toInstant(ZoneOffset.UTC).toEpochMilli();

        int result;
        try {
            if (Main.instance.databaseManager.select(uuid) == null)
                result = Main.instance.databaseManager.insert(uuid, pardonMillis, reason);
            else
                result = Main.instance.databaseManager.update(uuid, pardonMillis, reason);
        }catch(Exception e) {
            sender.sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return true;
        }

        if(result <= 0) {
            sender.sendMessage("§bː§f UUID §bː §rDB 에 플레이어 추가를 실패했습니다.");
            return true;
        }

        var pardonText = Main.getCalendarString(pardonTime);
        var periodText = Main.getCalendarString(now, pardonTime);
        for(var player : Bukkit.getOnlinePlayers()) {
            if(player.hasPermission(Objects.requireNonNull(Objects.requireNonNull(Main.instance.getCommand("uuid")).getPermission()))) {
                player.sendMessage("§bː§f UUID §bː §f" + target.getName() + "님이 " + pardonText+" 까지 정지되었습니다.");
                player.sendMessage(" 이유: " + reason);
//                player.sendMessage(" Ban ID: §e" + id);
                player.sendMessage(" 남은 일자: "+periodText);
                player.sendMessage("");
                player.sendMessage(" UUID: §7" + uuid);
            }
        }

        if(target.isOnline()) Objects.requireNonNull(target.getPlayer()).kickPlayer("§bː§f UUID §bː §f BAN!"
                + "\n\nReason: " + reason
                + "\n§fUntil: " + pardonText +
                "\n§fRemaining days: " + periodText);
        return true;
    }

    private boolean ban(CommandSender sender, String[] args, String uuid, OfflinePlayer target) {
        if(args.length < 3) {
            sender.sendMessage("§bː§f UUID §bː §r명령어를 확인해주세요.");
            return true;
        }

        var reason = getReasonFromText(args, 2);
        int result;
        try {
            if (Main.instance.databaseManager.select(uuid) == null)
                result = Main.instance.databaseManager.insert(uuid, reason);
            else
                result = Main.instance.databaseManager.update(uuid, reason);
        }catch(Exception e) {
            sender.sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return true;
        }

        if(result <= 0) {
            sender.sendMessage("§bː§f UUID §bː §rDB 에 플레이어 추가를 실패했습니다.");
            return true;
        }

        for(var player : Bukkit.getOnlinePlayers()) {
            if(player.hasPermission(Objects.requireNonNull(Objects.requireNonNull(Main.instance.getCommand("uuid")).getPermission()))) {
                player.sendMessage("§bː§f UUID §bː §r" + target.getName() + "님이 §c§n영구정지§f 되었습니다.");
                player.sendMessage(" Reason: " + reason);
//                player.sendMessage(" Ban ID: §e" + id);
                player.sendMessage("");
                player.sendMessage(" UUID: §7" + uuid);
            }
        }

        if(target.isOnline()) Objects.requireNonNull(target.getPlayer()).kickPlayer("§bː§f UUID §bː §f BAN!"
                + "\n\nReason: " + reason
                + "\n§fUntil: §c§n영구정지§f");
//                + "\n\nBan ID: §7" + id);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
        switch (args.length) {
            case 1 -> {
                return Arrays.asList("tban", "ban", "pardon", "list", "help");
            }
            case 2 -> {
                switch (args[0].toLowerCase()) {
                    case "tban", "ban", "pardon" -> {
                        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                    }
                    case "list" -> {
                        return List.of("{page}");
                    }
                }
            }
            case 3 -> {
                switch (args[0].toLowerCase()) {
                    case "ban" -> {
                        return List.of("[reason]");
                    }
                    case "tban" -> {
                        return List.of("[year]");
                    }
                }
            }
            case 4 -> {
                if (args[0].equalsIgnoreCase("tban")) {
                    return Arrays.asList("{month}", "[reason]");
                }
            }
            case 5 -> {
                if (args[0].equalsIgnoreCase("tban")) {
                    return Arrays.asList("{day}", "[reason]");
                }
            }
            case 6 -> {
                if (args[0].equalsIgnoreCase("tban")) {
                    return Arrays.asList("{hour}", "[reason]");
                }
            }
            case 7 -> {
                if (args[0].equalsIgnoreCase("tban")) {
                    return Arrays.asList("{minute}", "[reason]");
                }
            }
            case 8 -> {
                if (args[0].equalsIgnoreCase("tban")) {
                    return List.of("[reason]");
                }
            }
        }
        return null;
    }
}