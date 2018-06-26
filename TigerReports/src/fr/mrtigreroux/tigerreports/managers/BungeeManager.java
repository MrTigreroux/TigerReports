package fr.mrtigreroux.tigerreports.managers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class BungeeManager implements PluginMessageListener {
	
	private TigerReports main;
	private boolean initialized = false;
	private String serverName = null;
	
	public BungeeManager(TigerReports main) {
		this.main = main;
	}
	
	public void initialize() {
		if(ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "BungeeCord.Enabled")) {
			Messenger messenger = main.getServer().getMessenger();
			messenger.registerOutgoingPluginChannel(main, "BungeeCord");
			messenger.registerIncomingPluginChannel(main, "BungeeCord", this);
			initialized = true;
			Bukkit.getLogger().info(ConfigUtils.getInfoMessage("The plugin is using BungeeCord.", "Le plugin utilise BungeeCord."));
		} else Bukkit.getLogger().info(ConfigUtils.getInfoMessage("The plugin is not using BungeeCord.", "Le plugin n'utilise pas BungeeCord."));
	}
	
	public void collectServerName() {
		if(serverName == null) sendPluginMessage("GetServer");
	}
	
	public void collectDelayedlyServerName() {
		if(serverName == null) {
			Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {
				
				@Override
				public void run() {
					sendPluginMessage("GetServer");
				}
				
			}, 5);
		}
	}
	
	public String getServerName() {
		if(serverName == null) sendPluginMessage("GetServer");
		return serverName != null ? serverName : "localhost";
	}
	
	public void sendServerPluginNotification(String serverName, String message) {
		if(!initialized) return;
		
		Player p = getRandomPlayer();
		if(p == null) return;
		try {
			ByteArrayDataOutput out = ByteStreams.newDataOutput();
			out.writeUTF("Forward");
			out.writeUTF(serverName);
			out.writeUTF("TigerReports");
			
			ByteArrayOutputStream messageOut = new ByteArrayOutputStream();
			DataOutputStream messageStream = new DataOutputStream(messageOut);
			messageStream.writeUTF(message);
			
			byte[] messageBytes = messageOut.toByteArray();
			out.writeShort(messageBytes.length);
			out.write(messageBytes);
			
			p.sendPluginMessage(main, "BungeeCord", out.toByteArray());
		} catch (IOException ignored) {}
	}

	public void sendPluginNotification(String message) {
		sendServerPluginNotification("ALL", System.currentTimeMillis()+" "+message);
	}
	
	public void sendPluginMessage(String... message) {
		if(!initialized) return;
		
		Player p = getRandomPlayer();
		if(p == null) return;
		
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		for(String part : message) out.writeUTF(part);
		p.sendPluginMessage(main, "BungeeCord", out.toByteArray());
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player p, byte[] messageReceived) {
		if(!channel.equals("BungeeCord")) return;
		
		ByteArrayDataInput in = ByteStreams.newDataInput(messageReceived);
		String subchannel = in.readUTF();
		if(subchannel.equals("TigerReports")) {
			byte[] messageBytes = new byte[in.readShort()];
			in.readFully(messageBytes);

			DataInputStream messageStream = new DataInputStream(new ByteArrayInputStream(messageBytes));
			try {
				String message = messageStream.readUTF();
				int index = message.indexOf(' ');
				long sendTime = Long.parseLong(message.substring(0, index));
				boolean notify = System.currentTimeMillis()-sendTime < 20000;
				
				message = message.substring(index+1);
				String[] parts = message.split(" ");
				Report r = parts.length == 3 ? ReportUtils.getReportById(Integer.parseInt(parts[2])) : null;
				User u = parts.length == 4 || parts.length == 5 ? UserUtils.getUser(parts[3]) : null;
				
				switch(parts[1]) {
					case "new_report": ReportUtils.sendReport(new Report(Integer.parseInt(parts[0]), Status.WAITING.getConfigWord(), "None", parts[2].replace("_", " "), parts[3], parts[4], parts[5].replace("_", " ")), parts[6], notify); break;
					case "new_status": r.setStatus(Status.valueOf(parts[0]), true); break;
					case "process": r.process(parts[0].split("/")[0], notify ? parts[0].split("/")[1] : null, parts[3], true, false); break;
					case "delete": r.delete(notify ? parts[0] : null, true); break;
					case "archive": r.archive(notify ? parts[0] : null, true); break;
					case "unarchive": r.unarchive(notify ? parts[0] : null, true); break;
					case "delete_archive": r.deleteFromArchives(notify ? parts[0] : null, true); break;
					
					case "new_immunity": u.updateImmunity(parts[0].equals("null") ? null : parts[0].replace("_", " "), true); break;
					case "new_cooldown": u.updateCooldown(parts[0].equals("null") ? null : parts[0].replace("_", " "), true); break;
					case "punish": u.punish(Double.parseDouble(parts[4]), notify ? parts[0] : null, true); break;
					case "stop_cooldown": u.stopCooldown(notify ? parts[0] : null, true); break;
					case "change_statistic": u.changeStatistic(parts[2], Integer.parseInt(parts[0]), true); break;
					case "teleport": UserUtils.getPlayer(parts[0]).teleport(MessageUtils.getConfigLocation(parts[2])); break;
					default: break;
				}
			} catch (Exception ignored) {}
		} else if(subchannel.equals("GetServer")) serverName = in.readUTF();
	}
	
	private Player getRandomPlayer() {
		return Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
	}
	
}
