package fr.mrtigreroux.tigerreports.objects.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Statistic;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public abstract class User {

	protected final String uuid;
	protected String name = null;
	protected String immunity = null;
	protected String cooldown = null;
	protected Map<String, Integer> statistics = null;
	
	public User(String uuid) {
		this.uuid = uuid;
	}
	
	public String getName() {
		if(name == null) name = UserUtils.getName(uuid);
		save();
		return name;
	}
	
	public abstract void sendMessage(Object message);
	
	public void updateImmunity(String immunity, boolean bungee) {
		this.immunity = immunity;
		save();
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification((immunity != null ? immunity.replace(" ", "_") : "null")+" new_immunity user "+uuid);
			TigerReports.getDb().updateAsynchronously("UPDATE users SET immunity = ? WHERE uuid = ?", Arrays.asList(immunity, uuid));
		}
	}
	
	public void startImmunity(boolean bungee) {
		updateImmunity(MessageUtils.convertToDate(MessageUtils.getSeconds(MessageUtils.getNowDate())+ConfigUtils.getReportedImmunity()), bungee);
	}
	
	public String getImmunity() {
		if(immunity == null) {
			immunity = (String) TigerReports.getDb().query("SELECT immunity FROM users WHERE uuid = ?", Arrays.asList(uuid)).getResult(0, "immunity");
			save();
		}
		
		if(immunity == null) {
			immunity = "|";
			save();
			return null;
		} else if(immunity.equals("always")) return immunity;
		else if(immunity.equals("|")) return null;
		else {
			double seconds = MessageUtils.getSeconds(immunity)-MessageUtils.getSeconds(MessageUtils.getNowDate());
			return seconds > 0 ? MessageUtils.convertToSentence(seconds) : null;
		}
	}
	
	public void updateCooldown(String cooldown, boolean bungee) {
		this.cooldown = cooldown;
		save();
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification((cooldown != null ? cooldown.replace(" ", "_") : "null")+" new_cooldown user "+uuid);
			TigerReports.getDb().updateAsynchronously("UPDATE users SET cooldown = ? WHERE uuid = ?", Arrays.asList(cooldown, uuid));
		}
	}
	
	public void startCooldown(double seconds, boolean bungee) {
		updateCooldown(MessageUtils.convertToDate(MessageUtils.getSeconds(MessageUtils.getNowDate())+seconds), bungee);
	}
	
	public void punish(double seconds, String player, boolean bungee) {
		String time = MessageUtils.convertToSentence(seconds);
		MessageUtils.sendStaffMessage(Message.STAFF_PUNISH.get().replace("_Player_", player).replace("_Signalman_", getName()).replace("_Time_", time), ConfigSound.STAFF.get());
		sendMessage(Message.PUNISHED.get().replace("_Time_", time));
		if(!bungee) TigerReports.getBungeeManager().sendPluginNotification(player+" punish user "+uuid+" "+seconds);
		startCooldown(seconds, bungee);
	}
	
	public String getCooldown() {
		if(cooldown == null || (cooldown.startsWith("|") && System.currentTimeMillis()-Long.parseLong(cooldown.replace("|", "")) > 300000)) {
			cooldown = (String) TigerReports.getDb().query("SELECT cooldown FROM users WHERE uuid = ?", Arrays.asList(uuid)).getResult(0, "cooldown");
			save();
		}
		
		if(cooldown == null) {
			cooldown = "|"+System.currentTimeMillis();
			save();
			return null;
		} else if(cooldown.startsWith("|")) return null;
		double seconds = MessageUtils.getSeconds(cooldown)-MessageUtils.getSeconds(MessageUtils.getNowDate());
		return seconds > 0 ? MessageUtils.convertToSentence(seconds) : "None";
	}
	
	public void stopCooldown(String player, boolean bungee) {
		MessageUtils.sendStaffMessage(Message.STAFF_STOPCOOLDOWN.get().replace("_Player_", player).replace("_Target_", getName()), ConfigSound.STAFF.get());
		sendMessage(Message.COOLDOWN_STOPPED.get());
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(player+" stop_cooldown user "+uuid);
			updateCooldown(null, bungee);
		}
	}

	public Map<String, Integer> getStatistics() {
		if(statistics != null) return statistics;
		statistics = new HashMap<String, Integer>();
		Map<String, Object> result = TigerReports.getDb().query("SELECT true_appreciations,uncertain_appreciations,false_appreciations,reports,reported_times,processed_reports FROM users WHERE uuid = ?", Arrays.asList(uuid)).getResult(0);
		for(Statistic statistic : Statistic.values()) {
			String statName = statistic.getConfigName();
			statistics.put(statName, result != null ? (Integer) result.get(statName) : 0);
		}
		save();
		return statistics;
	}
	
	public void changeStatistic(String statistic, int value, boolean bungee) {
		if(statistics == null) getStatistics();
		statistics.put(statistic, statistics.get(statistic)+value);
		save();
		if(!bungee) {
			TigerReports.getBungeeManager().sendPluginNotification(value+" change_statistic "+statistic+" "+uuid);
			TigerReports.getDb().updateAsynchronously("UPDATE users SET "+statistic+" = "+statistic+" + ? WHERE uuid = ?", Arrays.asList(value, uuid));
		}
	}

	public List<String> getNotifications() {
		try {
			return Arrays.asList(((String) TigerReports.getDb().query("SELECT notifications FROM users WHERE uuid = ?", Arrays.asList(uuid)).getResult(0, "notifications")).split("#next#"));
		} catch (Exception noNotifications) {
			return new ArrayList<String>();
		}
	}
	
	public void setNotifications(List<String> notifications) {
		TigerReports.getDb().updateAsynchronously("UPDATE users SET notifications = ? WHERE uuid = ?", Arrays.asList(String.join("#next#", notifications), uuid));
	}
	
	public void save() {
		TigerReports.Users.put(uuid, this);
	}
	
}
