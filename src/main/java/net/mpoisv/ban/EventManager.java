package net.mpoisv.ban;

import net.mpoisv.ban.command.Uuid;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.time.Instant;
import java.time.ZoneOffset;

public class EventManager implements Listener {
    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        DatabaseManager.Data data;
        try {
            data = Main.instance.databaseManager.select(event.getUniqueId().toString());
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§bː§f UUID §bː §rLogin Event -> DB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return;
        }
        if (data == null) return;
        if (data.getTime() <= 0) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    "§bː§f UUID §bː §f BAN!" +
                            "\n\nReason: " + data.getReason() +
                            "\n§fUntil: §c§n영구정지§f");
            return;
        }
        var millis = System.currentTimeMillis();
        if (data.getTime() <= millis) {
            try {
                Main.instance.databaseManager.delete(data.getUuid());
            } catch (Exception e) {
                Bukkit.getConsoleSender().sendMessage("§bː§f UUID §bː §rLogin Event -> DB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return;
            }
        }
        var now = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDateTime();
        var pardonTime = Instant.ofEpochMilli(data.getTime()).atZone(ZoneOffset.UTC).toLocalDateTime();
        var pardonText = Main.getCalendarString(pardonTime);
        var periodText = Main.getCalendarString(now, pardonTime);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                "§bː§f UUID §bː §f BAN!" +
                        "\n\nReason: " + data.getReason() +
                        "\n§fUntil: " + pardonText +
                        "\n§fRemaining days: " + periodText);
    }

    @EventHandler
    public void onInventory(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getView().getTitle().startsWith(Uuid.BAN_GUI_PREFIX)
                && (event.getView().getTitle().endsWith(Uuid.BAN_GUI_LIST_END) || event.getView().getTitle().endsWith(Uuid.BAN_GUI_PARDON_END))))
            return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory() || event.getCurrentItem() == null) return;
        if (event.getSlot() == 52 || event.getSlot() == 53) {
            var pageInfo = event.getCurrentItem().getItemMeta().getLore().get(0).substring(8);
            var page = Integer.parseInt(pageInfo.split("/")[0]);

            DatabaseManager.Pagination pagination;
            try {
                pagination = Main.instance.databaseManager.getPagination(Math.max(1, page + (event.getSlot() == 52 ? -1 : 1)), 52);
            } catch (Exception e) {
                event.getWhoClicked().sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return;
            }
            if (event.getView().getTitle().endsWith(Uuid.BAN_GUI_LIST_END))
                event.getWhoClicked().openInventory(Main.getListInventory(pagination, Uuid.BAN_GUI_LIST_TITLE.replace("{p}", Integer.toString(pagination.getCurrentPage())), false));
            else
                event.getWhoClicked().openInventory(Main.getListInventory(pagination, Uuid.BAN_GUI_PARDON_TITLE.replace("{p}", Integer.toString(pagination.getCurrentPage())), true));
        } else if (event.getView().getTitle().endsWith(Uuid.BAN_GUI_PARDON_END)) {
            var lore = event.getCurrentItem().getItemMeta().getLore();
            if (lore.size() < 3 || !lore.get(lore.size() - 3).startsWith("§fUUID: §7")) return;
            var uuid = lore.get(lore.size() - 3).substring(10);

            int result;
            try {
                result = Main.instance.databaseManager.delete(uuid);
            } catch (Exception e) {
                event.getWhoClicked().sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return;
            }

            if (result <= 0) {
                event.getWhoClicked().sendMessage("§bː§f UUID §bː §r밴된 유저가 아니거나 삭제를 실패했습니다.");
                return;
            }
            event.getWhoClicked().sendMessage("§bː§f UUID §bː §r" + event.getCurrentItem().getItemMeta().getDisplayName().substring(2) + "(" + uuid + ") 님이 언밴 되었습니다.");

            var pageInfo = event.getView().getTitle().substring(Uuid.BAN_GUI_PREFIX.length());
            pageInfo = pageInfo.split(" ")[0];
            var page = Integer.parseInt(pageInfo);

            DatabaseManager.Pagination pagination;
            try {
                pagination = Main.instance.databaseManager.getPagination(Math.max(1, page), 52);
            } catch (Exception e) {
                event.getWhoClicked().sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return;
            }

            event.getWhoClicked().openInventory(Main.getListInventory(pagination, Uuid.BAN_GUI_PARDON_TITLE.replace("{p}", Integer.toString(pagination.getCurrentPage())), true));
        }
    }
}
