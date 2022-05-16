package fr.mrtigreroux.tigerreports.utils;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.google.common.primitives.Ints;

import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.events.NewReportEvent;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * @author MrTigreroux
 */

public class ReportUtils {

	private ReportUtils() {}

	/**
	 * @param ru the online reported user instance
	 * @return true if reported data has been successfully collected.
	 */
	@SuppressWarnings("deprecation")
	public static boolean collectAndFillReportedData(User ru, BungeeManager bm, Map<String, Object> data) {
		Player rp = ru.getPlayer();
		if (rp == null) {
			return false;
		}
		try {
			data.put("reported_ip", rp.getAddress().getAddress().toString());
			data.put("reported_location", MessageUtils.formatConfigLocation(rp.getLocation(), bm));
			data.put("reported_messages", ru.getLastMessages());
			data.put("reported_gamemode", rp.getGameMode().toString().toLowerCase());
			data.put("reported_on_ground",
			        !rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR) ? 1 : 0);
			data.put("reported_sneak", rp.isSneaking() ? 1 : 0);
			data.put("reported_sprint", rp.isSprinting() ? 1 : 0);
			data.put("reported_health", (int) Math.round(rp.getHealth()) + "/" + (int) Math.round(rp.getMaxHealth()));
			data.put("reported_food", rp.getFoodLevel());
			data.put("reported_effects", MessageUtils.formatConfigEffects(rp.getActivePotionEffects()));
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	@SuppressWarnings("deprecation")
	public static void sendReport(Report r, String server, boolean notify, Database db, VaultManager vm,
	        BungeeManager bm) {
		if (r.isStackedReport() && !ConfigUtils.isEnabled("Config.NotifyStackedReports")) {
			return;
		}

		try {
			Bukkit.getServer().getPluginManager().callEvent(new NewReportEvent(server, r));
		} catch (Exception ignored) {}

		if (r.isArchived() || !notify) {
			return;
		}

		int reportId = r.getId();

		BaseComponent alert = new TextComponent("");
		alert.setColor(ChatColor.valueOf(MessageUtils.getLastColor(Message.ALERT.get(), "_Reason_").name()));
		if (reportId == -1) {
			MessageUtils.sendStaffMessage(
			        Message.STAFF_MAX_REPORTS_REACHED.get().replace("_Amount_", Integer.toString(getMaxReports())),
			        ConfigSound.STAFF.get());
		} else {
			alert.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/reports #" + reportId));
			BaseComponent hoverTC = new TextComponent("");
			MessageUtils.APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD.accept(hoverTC,
			        Message.ALERT_DETAILS.get()
			                .replace("_Report_", r.getName())
			                .replace(ConfigUtils.getLineBreakSymbol(), "\n"));
			alert.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new BaseComponent[] { hoverTC }));
		}

		String[] lines = Message.ALERT.get()
		        .replace("_Server_", MessageUtils.getServerName(server))
		        .replace("_Reporter_",
		                r.getPlayerName(r.getLastReporter(), Report.ParticipantType.REPORTER, false, true, vm, bm))
		        .replace("_Reported_",
		                r.getPlayerName(Report.ParticipantType.REPORTED, !ReportUtils.onlinePlayerRequired(), true, vm,
		                        bm))
		        .replace("_Reason_", r.getReason(false))
		        .split(ConfigUtils.getLineBreakSymbol());

		for (String line : lines) {
			MessageUtils.APPEND_TEXT_WITH_TRANSLATED_COLOR_CODES_TO_COMPONENT_BUILDER_METHOD.accept(alert, line);
			MessageUtils.sendStaffMessage(new TextComponent(alert), ConfigSound.REPORT.get());
		}
	}

	public static int getTotalReports(Database db) {
		Object o = db.query("SELECT COUNT(report_id) AS total FROM tigerreports_reports", null).getResult(0, "total");
		return o instanceof Integer ? (int) o : Ints.checkedCast((long) o);
	}

	public static int getMaxReports() {
		return ConfigFile.CONFIG.get().getInt("Config.MaxReports", 100);
	}

	public static boolean permissionRequiredToReport() {
		return ConfigUtils.isEnabled("Config.PermissionRequired");
	}

	public static boolean onlinePlayerRequired() {
		return ConfigUtils.isEnabled("Config.ReportOnline");
	}

	public static int getMinCharacters() {
		return ConfigFile.CONFIG.get().getInt("Config.MinCharacters", 4);
	}

	public static long getCooldown() {
		return ConfigFile.CONFIG.get().getLong("Config.ReportCooldown", 300);
	}

	public static long getAbusiveReportCooldown() {
		return ConfigFile.CONFIG.get().getLong("Config.AbusiveReport.Cooldown", 3600);
	}

	public static boolean onlyDoneArchives() {
		return ConfigUtils.isEnabled("Config.OnlyDoneArchives");
	}

	public static boolean stackReports() {
		return ConfigUtils.isEnabled("Config.StackReports");
	}

	public static boolean punishmentsEnabled() {
		return ConfigUtils.isEnabled("Config.Punishments.Enabled");
	}

}
