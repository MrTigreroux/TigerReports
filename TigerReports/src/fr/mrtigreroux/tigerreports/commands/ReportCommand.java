package fr.mrtigreroux.tigerreports.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.managers.BungeeManager;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.OnlineUser;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class ReportCommand implements TabExecutor {

	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		if (!UserUtils.checkPlayer(s) || (ReportUtils.permissionRequired() && !Permission.REPORT.check(s)))
			return true;

		TigerReports tr = TigerReports.getInstance();

		Player p = (Player) s;
		OnlineUser u = tr.getUsersManager().getOnlineUser(p);
		String uuid = p.getUniqueId().toString();

		String cooldown = u.getCooldown();
		if (cooldown != null) {
			MessageUtils.sendErrorMessage(p, Message.COOLDOWN.get().replace("_Time_", cooldown));
			return true;
		}

		if (args.length == 0 || (args.length == 1 && !ConfigUtils.exist(ConfigFile.CONFIG.get(), "Config.DefaultReasons.Reason1"))) {
			s.sendMessage(Message.get("ErrorMessages.Invalid-syntax-report"));
			return true;
		}

		String reportedName = args[0];
		if (reportedName.equalsIgnoreCase(p.getName()) && !Permission.MANAGE.isOwned(u)) {
			MessageUtils.sendErrorMessage(p, Message.REPORT_ONESELF.get());
			return true;
		}

		Player rp = UserUtils.getPlayer(reportedName);
		String ruuid = UserUtils.getUniqueId(reportedName);
		if (rp == null) {
			if (!UserUtils.isValid(ruuid)) {
				MessageUtils.sendErrorMessage(p, Message.INVALID_PLAYER.get().replace("_Player_", reportedName));
				return true;
			}
		} else {
			reportedName = rp.getName();
		}

		if (ReportUtils.onlinePlayerRequired() && !UserUtils.isOnline(reportedName)) {
			MessageUtils.sendErrorMessage(p, Message.REPORTED_OFFLINE.get().replace("_Player_", reportedName));
			return true;
		}

		User ru = tr.getUsersManager().getUser(ruuid);
		String reportedImmunity = ru.getImmunity();
		if (reportedImmunity != null && !reportedName.equalsIgnoreCase(p.getName())) {
			if (reportedImmunity.equals("always")) {
				MessageUtils.sendErrorMessage(p, Message.PERMISSION_REPORT.get().replace("_Player_", reportedName));
			} else {
				MessageUtils.sendErrorMessage(p, Message.PLAYER_ALREADY_REPORTED.get()
						.replace("_Player_", reportedName)
						.replace("_Time_", reportedImmunity));
			}
			return true;
		}

		if (args.length == 1) {
			u.openReasonMenu(1, ru);
			return true;
		}

		StringBuilder sb = new StringBuilder();
		for (int argNumber = 1; argNumber < args.length; argNumber++)
			sb.append(args[argNumber]).append(" ");
		String reason = sb.toString().trim();
		if (reason.length() < ReportUtils.getMinCharacters()) {
			MessageUtils.sendErrorMessage(p, Message.TOO_SHORT_REASON.get().replace("_Reason_", reason));
			return true;
		}
		if (ConfigUtils.getLineBreakSymbol().length() >= 1)
			reason = reason.replace(ConfigUtils.getLineBreakSymbol(), ConfigUtils.getLineBreakSymbol().substring(0, 1));

		if (!ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.CustomReasons")) {
			for (int reasonNumber = 1; reasonNumber <= 100; reasonNumber++) {
				String defaultReason = ConfigFile.CONFIG.get().getString("Config.DefaultReasons.Reason"+reasonNumber+".Name");
				if (defaultReason == null) {
					u.openReasonMenu(1, ru);
					return true;
				} else if (reason.equals(defaultReason)) {
					break;
				}
			}
		}

		String freason = reason;
		String freportedName = reportedName;
		String date = MessageUtils.getNowDate();

		Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

			@Override
			public void run() {
				Database db = tr.getDb();

				int reportId = -1;
				Report r = null;
				if (ReportUtils.stackReports()) {
					Map<String, Object> result = db.query(
							"SELECT report_id,status,appreciation,date,reported_uuid,reporter_uuid,reason FROM tigerreports_reports WHERE status NOT LIKE ? AND reported_uuid = ? AND archived = ? AND LOWER(reason) = LOWER(?) LIMIT 1",
							Arrays.asList(Status.DONE.getConfigWord()+"%", ruuid, 0, freason)).getResult(0);
					if (result != null) {
						try {
							String reporterUuid = (String) result.get("reporter_uuid");
							if (reporterUuid.contains(uuid)) {
								Bukkit.getScheduler().runTask(tr, new Runnable() {

									@Override
									public void run() {
										MessageUtils.sendErrorMessage(p, Message.get("ErrorMessages.Player-already-reported-by-you")
												.replace("_Player_", freportedName)
												.replace("_Reason_", freason));
									}

								});
								return;
							}

							reporterUuid += ","+uuid;
							result.put("reporter_uuid", reporterUuid);
							r = ReportUtils.formatEssentialOfReport(result);
							if (r != null) {
								reportId = r.getId();
								if (ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "Config.UpdateDateOfStackedReports")) {
									db.update("UPDATE tigerreports_reports SET reporter_uuid = ?, date = ? WHERE report_id = ?", Arrays.asList(
											reporterUuid, date, reportId));
								} else {
									db.update("UPDATE tigerreports_reports SET reporter_uuid = ? WHERE report_id = ?", Arrays.asList(reporterUuid,
											reportId));
								}
							}

						} catch (Exception invalidReport) {}
					}
				}

				boolean missingData = false;
				if (r == null) {
					reportId = (reportId = ReportUtils.getTotalReports()+1) <= ReportUtils.getMaxReports() ? reportId : -1;

					if (reportId != -1) {
						List<Object> parameters;
						if (rp != null) {
							parameters = Arrays.asList(Status.WAITING.getConfigWord(), "None", date, ruuid, uuid, freason, rp.getAddress()
									.getAddress()
									.toString(), MessageUtils.formatConfigLocation(rp.getLocation()), ru.getLastMessages(), rp.getGameMode()
											.toString()
											.toLowerCase(), !rp.getLocation().getBlock().getRelative(BlockFace.DOWN).getType().equals(Material.AIR),
									rp.isSneaking(), rp.isSprinting(), (int) Math.round(rp.getHealth())+"/"+(int) Math.round(rp.getMaxHealth()), rp
											.getFoodLevel(), MessageUtils.formatConfigEffects(rp.getActivePotionEffects()), p.getAddress()
													.getAddress()
													.toString(), MessageUtils.formatConfigLocation(p.getLocation()), u.getLastMessages());
						} else {
							missingData = true;
							parameters = Arrays.asList(Status.WAITING.getConfigWord(), "None", date, ruuid, uuid, freason, null, null, ru
									.getLastMessages(), null, null, null, null, null, null, null, p.getAddress().toString(), MessageUtils
											.formatConfigLocation(p.getLocation()), u.getLastMessages());
						}
						reportId = db.insert(
								"INSERT INTO tigerreports_reports (status,appreciation,date,reported_uuid,reporter_uuid,reason,reported_ip,reported_location,reported_messages,reported_gamemode,reported_on_ground,reported_sneak,reported_sprint,reported_health,reported_food,reported_effects,reporter_ip,reporter_location,reporter_messages) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);",
								parameters);
					}

					r = new Report(reportId, Status.WAITING.getConfigWord(), "None", date, ruuid, uuid, freason);
				}

				boolean fmissingData = missingData;
				Report fr = r;

				Bukkit.getScheduler().runTask(tr, new Runnable() {

					@Override
					public void run() {
						BungeeManager bm = tr.getBungeeManager();
						String server = bm.getServerName();

						ReportUtils.sendReport(fr, server, true);
						s.sendMessage(Message.REPORT_SENT.get()
								.replace("_Player_", fr.getPlayerName("Reported", false, false))
								.replace("_Reason_", freason));
						bm.sendPluginNotification(fr.getId()+" new_report "+date.replace(" ", "_")+" "+ruuid+" "+fr.getLastReporterUniqueId()+" "
								+freason.replace(" ", "_")+" "+server+" "+fmissingData);

						u.startCooldown(ReportUtils.getCooldown(), false);
						ru.startImmunity(false);
						u.changeStatistic("reports", 1);
						ru.changeStatistic("reported_times", 1);

						String reported = fr.getPlayerName("Reported", false, false);

						for (String command : ConfigFile.CONFIG.get().getStringList("Config.AutoCommands"))
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("_Server_", server)
									.replace("_Date_", date)
									.replace("_Reporter_", p.getName())
									.replace("_Reported_", reported)
									.replace("_Reason_", freason));
					}
				});

			}

		});

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] args) {
		return args.length == 1 ? StringUtil.copyPartialMatches(args[0], UserUtils.getOnlinePlayers(true), new ArrayList<>()) : new ArrayList<>();
	}

}
