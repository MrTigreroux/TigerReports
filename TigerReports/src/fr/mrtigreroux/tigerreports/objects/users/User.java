package fr.mrtigreroux.tigerreports.objects.users;

import java.util.*;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.*;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public abstract class User {

	protected final String uuid;
	protected String name = null;
	protected String displayName = null;
	private String immunity = null;
	protected String cooldown = null;
	private Map<String, Integer> statistics = null;
	public List<String> lastMessages = new ArrayList<>();

	public User(String uuid) {
		this.uuid = uuid;
	}

	public String getUniqueId() {
		return uuid;
	}

	public String getName() {
		if (name == null)
			name = UserUtils.getName(uuid);
		return name;
	}

	public String getDisplayName(boolean staff) {
		if (displayName == null)
			displayName = UserUtils.getDisplayName(uuid, staff);
		return displayName;
	}

	public abstract void sendMessage(Object message);

	public void updateImmunity(String immunity, boolean bungee) {
		this.immunity = immunity;

		TigerReports tr = TigerReports.getInstance();
		if (immunity != null && immunity.equals("always"))
			tr.getUsersManager().addExemptedPlayer(getName());
		else
			tr.getUsersManager().removeExemptedPlayer(getName());

		if (!bungee) {
			tr.getBungeeManager()
			        .sendPluginNotification(
			                (immunity != null ? immunity.replace(" ", "_") : "null") + " new_immunity user " + uuid);
			Database db = tr.getDb();
			if (db != null)
				db.updateAsynchronously("UPDATE tigerreports_users SET immunity = ? WHERE uuid = ?",
				        Arrays.asList(immunity, uuid));
		}
	}

	public void startImmunity(boolean bungee) {
		updateImmunity(MessageUtils.getRelativeDate(ConfigFile.CONFIG.get().getLong("Config.ReportedImmunity", 120)),
		        bungee);
	}

	public String getImmunity() {
		if (this instanceof OnlineUser)
			immunity = Permission.REPORT_EXEMPT.isOwned((OnlineUser) this) ? "always" : null;
		if (immunity == null) {
			immunity = (String) TigerReports.getInstance()
			        .getDb()
			        .query("SELECT immunity FROM tigerreports_users WHERE uuid = ?", Collections.singletonList(uuid))
			        .getResult(0, "immunity");
			if (immunity == null) {
				immunity = "|";
				return null;
			}
		}

		if (immunity.equals("always")) {
			return immunity;
		} else if (immunity.equals("|")) {
			return null;
		} else {
			double seconds = MessageUtils.getSecondsBetweenNowAndDate(immunity);
			return seconds > 0 ? MessageUtils.convertToSentence(seconds) : null;
		}
	}

	public void updateCooldown(String cooldown, boolean bungee) {
		this.cooldown = cooldown == null ? "|" + System.currentTimeMillis() : cooldown;
		if (!bungee) {
			TigerReports tr = TigerReports.getInstance();
			tr.getBungeeManager()
			        .sendPluginNotification(
			                (cooldown != null ? cooldown.replace(" ", "_") : "null") + " new_cooldown user " + uuid);
			tr.getDb()
			        .updateAsynchronously("UPDATE tigerreports_users SET cooldown = ? WHERE uuid = ?",
			                Arrays.asList(cooldown, uuid));
		}
	}

	public void startCooldown(long seconds, boolean bungee) {
		updateCooldown(MessageUtils.getRelativeDate(seconds), bungee);
	}

	public void punish(long seconds, String staffUuid, boolean bungee) {
		String time = MessageUtils.convertToSentence(seconds);
		if (staffUuid != null) {
			MessageUtils.sendStaffMessage(Message.STAFF_PUNISH.get()
			        .replace("_Player_", UserUtils.getDisplayName(staffUuid, true))
			        .replace("_Reporter_", getDisplayName(false))
			        .replace("_Time_", time), ConfigSound.STAFF.get());
			sendMessage(Message.PUNISHED.get().replace("_Time_", time));
		}
		if (!bungee)
			TigerReports.getInstance()
			        .getBungeeManager()
			        .sendPluginNotification(staffUuid + " punish user " + uuid + " " + seconds);
		startCooldown(seconds, bungee);
	}

	public String getCooldown() {
		if (cooldown == null || (cooldown.startsWith("|")
		        && System.currentTimeMillis() - Long.parseLong(cooldown.replace("|", "")) > 300000))
			cooldown = (String) TigerReports.getInstance()
			        .getDb()
			        .query("SELECT cooldown FROM tigerreports_users WHERE uuid = ?", Collections.singletonList(uuid))
			        .getResult(0, "cooldown");

		if (cooldown == null) {
			cooldown = "|" + System.currentTimeMillis();
		} else if (!cooldown.startsWith("|")) {
			double seconds = MessageUtils.getSecondsBetweenNowAndDate(cooldown);
			if (seconds > 0) {
				return MessageUtils.convertToSentence(seconds);
			} else {
				cooldown = "|" + System.currentTimeMillis();
			}
		}
		return null;
	}

	public void stopCooldown(String staffUuid, boolean bungee) {
		if (staffUuid != null) {
			MessageUtils.sendStaffMessage(Message.STAFF_STOPCOOLDOWN.get()
			        .replace("_Player_", UserUtils.getDisplayName(staffUuid, true))
			        .replace("_Target_", getDisplayName(false)), ConfigSound.STAFF.get());
			sendMessage(Message.COOLDOWN_STOPPED.get());
		}
		if (!bungee) {
			TigerReports.getInstance()
			        .getBungeeManager()
			        .sendPluginNotification(staffUuid + " stop_cooldown user " + uuid);
			updateCooldown(null, false);
		}
	}

	public Map<String, Integer> getStatistics() {
		if (statistics != null)
			return statistics;
		statistics = new HashMap<>();
		Map<String, Object> result = TigerReports.getInstance()
		        .getDb()
		        .query("SELECT true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM tigerreports_users WHERE uuid = ?",
		                Collections.singletonList(uuid))
		        .getResult(0);
		for (Statistic statistic : Statistic.values()) {
			String statName = statistic.getConfigName();
			statistics.put(statName, result != null ? (Integer) result.get(statName) : 0);
		}
		return statistics;
	}

	public void changeStatistic(String statistic, int relativeValue) {
		if (statistics == null)
			getStatistics();
		setStatistic(statistic, (statistics.get(statistic) != null ? statistics.get(statistic) : 0) + relativeValue,
		        false);
	}

	public void setStatistic(String statistic, int value, boolean bungee) {
		statistics.put(statistic, value);
		if (!bungee) {
			TigerReports tr = TigerReports.getInstance();
			tr.getBungeeManager().sendPluginNotification(value + " set_statistic " + statistic + " " + uuid);
			tr.getDb()
			        .updateAsynchronously("UPDATE tigerreports_users SET " + statistic + " = ? WHERE uuid = ?",
			                Arrays.asList(value, uuid));
		}
	}

	public List<String> getNotifications() {
		List<String> notifications = new ArrayList<>();
		try {
			notifications = new ArrayList<>(Arrays.asList(((String) TigerReports.getInstance()
			        .getDb()
			        .query("SELECT notifications FROM tigerreports_users WHERE uuid = ?",
			                Collections.singletonList(uuid))
			        .getResult(0, "notifications")).split("#next#")));
		} catch (Exception noNotifications) {}
		return notifications;
	}

	public void setNotifications(List<String> notifications) {
		TigerReports.getInstance()
		        .getDb()
		        .update("UPDATE tigerreports_users SET notifications = ? WHERE uuid = ?",
		                Arrays.asList(notifications != null ? String.join("#next#", notifications) : null, uuid));
	}

	public String getLastMessages() {
		return !lastMessages.isEmpty() ? String.join("#next#", lastMessages) : null;
	}

}
