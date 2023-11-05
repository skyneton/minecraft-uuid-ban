package net.mpoisv.ban;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;

public final class LanguageManager {

    private static String detectLanguageFile() {
        Locale aDefault = Locale.getDefault();
        if (aDefault.equals(Locale.JAPAN) || aDefault.equals(Locale.JAPANESE))
            return "lang-ja.yml";
        else if (aDefault.equals(Locale.KOREA) || aDefault.equals(Locale.KOREAN))
            return "lang-ko.yml";

        return "lang-en.yml";
    }

    public static void saveDefaultConfig(Plugin plugin) {
        var languageDir = new File(plugin.getDataFolder(), "lang");
        if(!languageDir.exists()) {
            languageDir.mkdirs();
            plugin.getConfig().set("language_file", detectLanguageFile());
            plugin.saveConfig();
        }

        plugin.saveResource("lang/lang-ko.yml", false);
        plugin.saveResource("lang/lang-en.yml", false);
        plugin.saveResource("lang/lang-ja.yml", false);
    }

    public static void loadConfig(File file) {
        if(!file.exists()) return;
        var config = YamlConfiguration.loadConfiguration(file);
        UnknownPlayer = config.getString("unknown_player", UnknownPlayer);

        BanReason = config.getString("ban_reason", BanReason);
        BanUntil = config.getString("ban_until", BanUntil);
        BanRemainDays = config.getString("ban_remain_days", BanRemainDays);
        BanInfinite = config.getString("ban_infinite", BanInfinite);

        ClickToUnBan = config.getString("click_to_unban", ClickToUnBan);

        BeforePage = config.getString("before_page", BeforePage);
        NextPage = config.getString("next_page", NextPage);

        CheckCommand = config.getString("check_command", CheckCommand);
        MojangApiError = config.getString("mojang_api_error", MojangApiError);
        ServerUUIDFailed = config.getString("server_uuid_failed", ServerUUIDFailed);

        CommandValueInfo = config.getString("command_value_info", CommandValueInfo);

        DBWorkFailed = config.getString("db_work_failed", DBWorkFailed);

        IsNotBanOrFailed = config.getString("is_not_ban_or_failed", IsNotBanOrFailed);

        UnbannedPlayer = config.getString("unbanned_player", UnbannedPlayer);

        DBPlayerUpdateFailed = config.getString("db_player_update_failed", DBPlayerUpdateFailed);

        TimeBanInfo = config.getString("time_ban_info", TimeBanInfo);
        BanInfo = config.getString("ban_info", BanInfo);
    }

    public static String UnknownPlayer = "Unknown Player";

    public static String BanReason = "Reason";
    public static String BanUntil = "Until";
    public static String BanRemainDays = "Remain days";
    public static String BanInfinite = "Infinite";

    public static String ClickToUnBan = "§fIf you click, §nUNBAN§f.";

    public static String BeforePage = "§bBefore Page";
    public static String NextPage = "§bNext Page";

    public static String CheckCommand = "Check command.";
    public static String MojangApiError = "Mojang API Server Error. Can't bring uuid. %error-message% : %error-cause%";
    public static String ServerUUIDFailed = "Can't bring UUID from server.";

    public static String CommandValueInfo = "[item] must use, {item} can use.";
    public static String DBWorkFailed = "DB work failed. %error-message% : %error-cause%";

    public static String IsNotBanOrFailed = "Is not banned player or delete failed.";

    public static String UnbannedPlayer = "%player%(%uuid%) is unbanned now.";

    public static String DBPlayerUpdateFailed = "Failed to add player to DB.";

    public static String TimeBanInfo = "%player% is banned until %until%.";
    public static String BanInfo = "%player% is banned §c§nforever§f.";
}
