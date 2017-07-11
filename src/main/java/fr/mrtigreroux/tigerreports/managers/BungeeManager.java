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
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class BungeeManager implements PluginMessageListener {
	
	private TigerReports main;
	private String serverName = null;
	
	public BungeeManager(TigerReports main) {
		this.main = main;
	}
	
	public void initialize() {
		Messenger messenger = main.getServer().getMessenger();
		messenger.registerOutgoingPluginChannel(main, "BungeeCord");
	    messenger.registerIncomingPluginChannel(main, "BungeeCord", this);
	    sendPluginMessage("GetServer");
	}
	
	public String getServerName() {
		if(serverName == null) sendPluginMessage("GetServer");
		return serverName != null ? serverName : "localhost";
	}
	
	public void sendServerPluginNotification(String serverName, String message) {
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
		} catch (IOException ex) {}
	}

	public void sendPluginNotification(String message) {
		sendServerPluginNotification("ALL", message);
	}
	
	public void sendPluginMessage(String... message) {
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
				String[] parts = message.split(" ");
				Report r = parts.length == 3 ? ReportUtils.getReportById(Integer.parseInt(parts[2])) : null;
				User u = parts.length == 4 || parts.length == 5 ? UserUtils.getUser(parts[3]) : null;
				
				switch(parts[1]) {
					case "new_report": ReportUtils.sendReport(new Report(Integer.parseInt(parts[0]), Status.WAITING.getConfigWord(), "None", parts[2].replace("_", " "), parts[3], parts[4], parts[5].replace("_", " "))); break;
					case "new_status": r.setStatus(Status.valueOf(parts[0]), true); break;
					case "process": r.process(parts[0].split("/")[0], parts[0].split("/")[1], parts[3], true); break;
					case "remove": r.remove(parts[0], true); break;
					case "archive": r.archive(parts[0], true); break;
					case "unarchive": r.unarchive(parts[0], true); break;
					case "remove_archive": r.removeFromArchives(parts[0], true); break;
					
					case "new_immunity": u.updateImmunity(parts[0].equals("null") ? null : parts[0].replace("_", " "), true); break;
					case "new_cooldown": u.updateCooldown(parts[0].equals("null") ? null : parts[0].replace("_", " "), true); break;
					case "punish": u.punish(Double.parseDouble(parts[4]), parts[0], true); break;
					case "stop_cooldown": u.stopCooldown(parts[0], true); break;
					case "change_statistic": u.changeStatistic(parts[2], Integer.parseInt(parts[0]), true); break;
					case "teleport": UserUtils.getPlayer(parts[0]).teleport(MessageUtils.getConfigLocation(parts[2])); break;
					default: break;
				}
			} catch (Exception ex) {}
		} else if(subchannel.equals("GetServer")) serverName = in.readUTF();
	}
	
	private Player getRandomPlayer() {
		return Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
	}
	
}
