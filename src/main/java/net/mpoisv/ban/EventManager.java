package net.mpoisv.ban;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;

public class EventManager implements Listener {
    @EventHandler
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        DatabaseManager.Data data;
        try {
            data = Main.instance.databaseManager.select(event.getUniqueId().toString());
        }catch(Exception e) {
            Bukkit.getConsoleSender().sendMessage("§bː§f UUID §bː §rLogin Event -> DB 작업을 실패했습니다. "
                    + e.getLocalizedMessage() + " : " + e.getCause());
            return;
        }
        if(data == null) return;
        if(data.getTime() <= 0) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    "§bː§f UUID §bː §f BAN!" +
                            "\n\nReason: " + data.getReason() +
                            "\n§fUntil: §c§n영구정지§f");
            return;
        }
        var millis = System.currentTimeMillis();
        if(data.getTime() <= millis) {
            try {
                Main.instance.databaseManager.delete(data.getUuid());
            }catch(Exception e) {
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
        if(event.getClickedInventory() == null) return;
        if(!(event.getView().getTitle().startsWith("§3") && event.getView().getTitle().endsWith("Ban List"))) return;
        event.setCancelled(true);
        if(event.getClickedInventory() != event.getView().getTopInventory() || event.getCurrentItem() == null) return;
        if(event.getSlot() == 52 || event.getSlot() == 53) {
            var pageInfo = event.getCurrentItem().getItemMeta().getLore().get(0).substring(8);
            var page = Integer.parseInt(pageInfo.split("/")[0]);

            DatabaseManager.Pagination pagination;
            try {
                pagination = Main.instance.databaseManager.getPagination(Math.max(1, page + (event.getSlot() == 52 ? -1 : 1)), 52);
            }catch (Exception e) {
                event.getWhoClicked().sendMessage("§bː§f UUID §bː §rDB 작업을 실패했습니다. "
                        + e.getLocalizedMessage() + " : " + e.getCause());
                return;
            }
            event.getWhoClicked().openInventory(Main.getListInventory(pagination, "§3" + pagination.getCurrentPage() + " Ban List"));
        }
    }
}
