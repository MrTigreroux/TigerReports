package fr.mrtigreroux.tigerreports.managers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Comment;
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

public class BungeeManager implements PluginMessageListener {

	private TigerReports plugin;
	private boolean initialized = false;
	private String serverName = null;
	private List<String> onlinePlayers = new ArrayList<>();
	private boolean onlinePlayersCollected = false;
	private String playerToRemove = null;

	public BungeeManager(TigerReports plugin) {
		this.plugin = plugin;
		initialize();
		collectServerName();
	}

	public void initialize() {
		if (ConfigUtils.isEnabled(ConfigFile.CONFIG.get(), "BungeeCord.Enabled")) {
			Messenger messenger = plugin.getServer().getMessenger();
			messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
			messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
			initialized = true;
			Bukkit.getLogger().info(ConfigUtils.getInfoMessage("The plugin is using BungeeCord.", "Le plugin utilise BungeeCord."));
		} else {
			Bukkit.getLogger().info(ConfigUtils.getInfoMessage("The plugin is not using BungeeCord.", "Le plugin n'utilise pas BungeeCord."));
		}
	}

	public void collectServerName() {
		if (serverName == null)
			sendPluginMessage("GetServer");
	}

	public void processPlayerConnection(String name) {
		if (!initialized)
			return;

		Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				collectServerName();
				if (!onlinePlayersCollected)
					collectOnlinePlayers();
				if (playerToRemove != null && playerToRemove != name) {
					sendPluginNotification(playerToRemove+" player_status false");
					playerToRemove = null;
				}
				updatePlayerStatus(name, true);
			}

		}, 5);
	}

	public void processPlayerDisconnection(String name) {
		if (!initialized)
			return;

		
		if (Bukkit.getOnlinePlayers().size() > 1) {
			updatePlayerStatus(name, false);
		} else {
			setPlayerStatus(name, false);
			playerToRemove = name;
		}
	}

	public String getServerName() {
		if (serverName == null)
			sendPluginMessage("GetServer");
		return serverName != null ? serverName : "localhost";
	}

	public void sendServerPluginNotification(String serverName, String message) {
		if (!initialized)
			return;

		Player p = getRandomPlayer();
		if (p == null)
			return;
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

			p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
		} catch (IOException ignored) {}
	}

	public void sendPluginNotification(String message) {
		sendServerPluginNotification("ALL", System.currentTimeMillis()+" "+message);
	}

	public void sendPluginMessage(String... message) {
		if (!initialized)
			return;

		Player p = getRandomPlayer();
		if (p == null)
			return;

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		for (String part : message)
			out.writeUTF(part);
		p.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] messageReceived) {
		if (!channel.equals("BungeeCord"))
			return;

		ByteArrayDataInput in = ByteStreams.newDataInput(messageReceived);
		String subchannel = in.readUTF();
		if (subchannel.equals("TigerReports")) {
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

				switch (parts[1]) {
					case "new_report":
						Report r = new Report(Integer.parseInt(parts[0]), Status.WAITING.getConfigWord(), "None", parts[2].replace("_", " "),
								parts[3], parts[4], parts[5].replace("_", " "));
						ReportUtils.sendReport(r, parts[6], notify);
						if (notify && parts[7].equals("true"))
							implementMissingData(r);
						break;
					case "new_status":
						getReport(parts).setStatus(Status.valueOf(parts[0]), true);
						break;
					case "process":
						getReport(parts).process(parts[0].split("/")[0], notify ? parts[0].split("/")[1] : null, parts[3], true, parts[4].equals(
								"true"));
						break;
					case "process_punish":
						String p4 = parts[4];
						getReport(parts).processPunishing(parts[0].split("/")[0], notify ? parts[0].split("/")[1] : null, true, parts[4].equals(
								"true"), message.substring(message.indexOf(p4)+p4.length()+1));
						break;
					case "delete":
						getReport(parts).delete(notify ? parts[0] : null, true);
						break;
					case "archive":
						getReport(parts).archive(notify ? parts[0] : null, true);
						break;
					case "unarchive":
						getReport(parts).unarchive(notify ? parts[0] : null, true);
						break;
					case "delete_archive":
						getReport(parts).deleteFromArchives(notify ? parts[0] : null, true);
						break;

					case "new_immunity":
						getUser(parts).updateImmunity(parts[0].equals("null") ? null : parts[0].replace("_", " "), true);
						break;
					case "new_cooldown":
						getUser(parts).updateCooldown(parts[0].equals("null") ? null : parts[0].replace("_", " "), true);
						break;
					case "punish":
						getUser(parts).punish(Double.parseDouble(parts[4]), notify ? parts[0] : null, true);
						break;
					case "stop_cooldown":
						getUser(parts).stopCooldown(notify ? parts[0] : null, true);
						break;
					case "set_statistic":
						getUser(parts).setStatistic(parts[2], Integer.parseInt(parts[0]), true);
						break;
					case "tp_loc":
						teleportDelayedly(parts[0], MessageUtils.getLocation(parts[2]));
						break;
					case "tp_player":
						if (!notify)
							break;

						String target = parts[2];
						Player t = UserUtils.getPlayer(target);
						if (t != null) {
							String staff = parts[0];
							sendPluginMessage("ConnectOther", staff, serverName);
							teleportDelayedly(staff, t.getLocation());
						}
						break;
					case "comment":
						Player rp = UserUtils.getPlayer(parts[3]);
						if (rp == null)
							break;

						OnlineUser ru = TigerReports.getInstance().getUsersManager().getOnlineUser(rp);
						Report report = TigerReports.getInstance().getReportsManager().getReportById(Integer.parseInt(parts[0]), false);
						Comment c = report.getCommentById(Integer.parseInt(parts[2]));
						((OnlineUser) ru).sendCommentNotification(report, c, true);
						break;

					case "player_status":
						String name = parts[0];
						boolean online = parts[2].equals("true");
						if (!online && UserUtils.getPlayer(name) != null) {
							updatePlayerStatus(name, true);
						} else {
							setPlayerStatus(name, online);
						}
						break;

					default:
						break;
				}
			} catch (Exception ignored) {}
		} else if (subchannel.equals("GetServer")) {
			serverName = in.readUTF();
		} else if (subchannel.equals("PlayerList")) {
			onlinePlayersCollected = true;
			in.readUTF();
			onlinePlayers = new ArrayList<>(Arrays.asList(in.readUTF().split(", ")));
		}
	}

	private Player getRandomPlayer() {
		return Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
	}

	private Report getReport(String[] parts) {
		return TigerReports.getInstance().getReportsManager().getReportById(Integer.parseInt(parts[2]), false);
	}

	private User getUser(String[] parts) {
		return TigerReports.getInstance().getUsersManager().getUser(parts[3]);
	}

	private void teleportDelayedly(String name, Location loc) {
		Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				Player p = UserUtils.getPlayer(name);
				if (name != null) {
					p.teleport(loc);
					ConfigSound.TELEPORT.play(p);
				}
			}

		}, 5);
	}

	private void implementMissingData(Report r) {
		Player rp = UserUtils.getPlayerFromUniqueId(r.getReportedUniqueId());
		if (rp == null)
			return;
		OnlineUser ru = plugin.getUsersManager().getOnlineUser(rp);
		plugin.getDb()
				.updateAsynchronously(
						"UPDATE tigerreports_reports SET reported_ip=?,reported_location=?,reported_messages=?,reported_gamemode=?,reported_on_ground=?,reported_sneak=?,reported_sprint=?,reported_health=?,reported_food=?,reported_effects=? WHERE report_id=?",
						Arrays.asList(rp.getAddress().getAddress().toString(), MessageUtils.formatConfigLocation(rp.getLocation()), ru
								.getLastMessages(), rp.getGameMode().toString().toLowerCase(), !rp.getLocation()
										.getBlock()
										.getRelative(BlockFace.DOWN)
										.getType()
										.equals(Material.AIR), rp.isSneaking(), rp.isSprinting(), (int) Math.round(rp.getHealth())+"/"+(int) Math
												.round(rp.getMaxHealth()), rp.getFoodLevel(), MessageUtils.formatConfigEffects(rp
														.getActivePotionEffects()), r.getId()));
	}

	public void collectOnlinePlayers() {
		onlinePlayersCollected = false;
		sendPluginMessage("PlayerList", "ALL");
	}

	public boolean isOnline(String name) {
		return onlinePlayers.contains(name);
	}

	public List<String> getOnlinePlayers() {
		return onlinePlayersCollected ? onlinePlayers : null;
	}

	private void setPlayerStatus(String name, boolean online) {
		if (online) {
			if (!onlinePlayers.contains(name))
				onlinePlayers.add(name);
		} else {
			onlinePlayers.remove(name);
		}
	}

	public void updatePlayerStatus(String name, boolean online) {
		if (!initialized)
			return;

		setPlayerStatus(name, online);
		sendPluginNotification(name+" player_status "+online);
	}

}
