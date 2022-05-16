package fr.mrtigreroux.tigerreports.managers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.reports.Report;
import fr.mrtigreroux.tigerreports.objects.reports.Report.StatusDetails;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class BungeeManager implements PluginMessageListener {

	public static final String MESSAGE_DATA_SEPARATOR = " ";
	public static final int RECENT_MESSAGE_MAX_DELAY = 5000;
	public static final int NOTIFY_MESSAGE_MAX_DELAY = 60000;

	private final TigerReports tr;
	private final ReportsManager rm;
	private final Database db;
	private final VaultManager vm;
	private final UsersManager um;
	private boolean initialized = false;
	private String serverName = null;
	private List<String> onlinePlayers = new ArrayList<>();
	private boolean onlinePlayersCollected = false;
	private String playerToRemove = null;

	public BungeeManager(TigerReports tr, ReportsManager rm, Database db, VaultManager vm, UsersManager um) {
		this.tr = tr;
		this.rm = rm;
		this.db = db;
		this.vm = vm;
		this.um = um;
		initialize();
	}

	private void initialize() {
		if (ConfigUtils.isEnabled("BungeeCord.Enabled")) {
			Messenger messenger = tr.getServer().getMessenger();
			messenger.registerOutgoingPluginChannel(tr, "BungeeCord");
			messenger.registerIncomingPluginChannel(tr, "BungeeCord", this);
			initialized = true;
			Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage("The plugin is using BungeeCord.",
			        "Le plugin utilise BungeeCord."));
		} else {
			Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage("The plugin is not using BungeeCord.",
			        "Le plugin n'utilise pas BungeeCord."));
		}
	}

	public void collectServerName() {
		if (serverName == null) {
			sendBungeeMessage("GetServer");
		}
	}

	public void processPlayerConnection(String name) {
		if (!initialized) {
			return;
		}

		tr.runTaskDelayedly(50L, new Runnable() {

			@Override
			public void run() {
				if (Bukkit.getPlayer(name) != null) {
					collectServerName();
					if (!onlinePlayersCollected) {
						collectOnlinePlayers();
					}
					if (playerToRemove != null) {
						if (playerToRemove != name) {
							sendPluginNotificationToAll(playerToRemove + " player_status false");
						}
						playerToRemove = null;
					}
					updatePlayerStatus(name, true);
				}
			}

		});
	}

	public void processPlayerDisconnection(String name) {
		if (!initialized) {
			return;
		}

		if (Bukkit.getOnlinePlayers().size() > 1) {
			updatePlayerStatus(name, false);
		} else {
			setPlayerStatus(name, false);
			playerToRemove = name;
		}
	}

	public String getServerName() {
		if (serverName == null) {
			sendBungeeMessage("GetServer");
		}
		return serverName != null ? serverName : "localhost";
	}

	public void sendUsersDataChanged(String... usersUUID) {
		if (usersUUID == null || usersUUID.length == 0) {
			return;
		}
		sendPluginNotificationToAll("users data_changed " + String.join(" ", usersUUID));
	}

	public void sendPluginNotificationToAll(String message) {
		if (message == null || message.isEmpty()) {
			throw new IllegalArgumentException("Empty message");
		}
		sendPluginMessageTo("ALL", System.currentTimeMillis() + MESSAGE_DATA_SEPARATOR + message);
	}

	public void sendPluginNotificationToAll(Object... messageParts) {
		sendPluginNotificationTo("ALL", messageParts);
	}

	public void sendPluginNotificationTo(String serverName, Object... messageParts) {
		if (messageParts == null || messageParts.length == 0) {
			throw new IllegalArgumentException("Empty message");
		}
		sendPluginMessageTo(serverName, System.currentTimeMillis() + MESSAGE_DATA_SEPARATOR
		        + MessageUtils.joinElements(MESSAGE_DATA_SEPARATOR, messageParts));
	}

	public void sendPluginMessageTo(String serverName, String message) {
		if (!initialized) {
			return;
		}

		Player p = getRandomPlayer();
		if (p == null) {
			return;
		}
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

			p.sendPluginMessage(tr, "BungeeCord", out.toByteArray());

			Logger.BUNGEE.info(() -> "<-- SENT (to: " + serverName + "): '" + message + "'");
		} catch (IOException ignored) {}
	}

	public void sendBungeeMessage(String... message) {
		if (!initialized) {
			return;
		}

		Player p = getRandomPlayer();
		if (p == null) {
			return;
		}

		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		for (String part : message) {
			out.writeUTF(part);
		}
		p.sendPluginMessage(tr, "BungeeCord", out.toByteArray());
	}

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] messageReceived) {
		if (!channel.equals("BungeeCord")) {
			return;
		}

		ByteArrayDataInput in = ByteStreams.newDataInput(messageReceived);
		String subchannel = in.readUTF();
		if (subchannel.equals("TigerReports")) {
			byte[] messageBytes = new byte[in.readShort()];
			in.readFully(messageBytes);

			DataInputStream messageStream = new DataInputStream(new ByteArrayInputStream(messageBytes));
			try {
				String fullMessage = messageStream.readUTF();
				int index = fullMessage.indexOf(' ');
				long sendTime = Long.parseLong(fullMessage.substring(0, index));
				long now = System.currentTimeMillis();
				long elapsedTime = now - sendTime;
				boolean isRecentMsg = elapsedTime < RECENT_MESSAGE_MAX_DELAY;
				boolean notify = elapsedTime < NOTIFY_MESSAGE_MAX_DELAY;

				String message = fullMessage.substring(index + 1);
				Logger.BUNGEE.info(() -> "--> RECEIVED (sent at: " + sendTime + "ms, elapsed: " + elapsedTime + "ms): '"
				        + message + "'");
				String[] parts = message.split(MESSAGE_DATA_SEPARATOR);

				switch (parts[1]) {
				case "new_report":
					int reportDataStartIndex = message.indexOf(parts[3]);
					String reportDataAsString = getReportDataAsString(message, reportDataStartIndex);
					processNewReportMessage(isRecentMsg, notify, parts[0], Boolean.parseBoolean(parts[2]),
					        reportDataAsString);
					break;
				case "new_status":
					getReportAsynchronously(parts[2], true, db, new ResultCallback<Report>() {

						@Override
						public void onResultReceived(Report r) {
							if (r != null) {
								Report.StatusDetails.asynchronouslyFrom(parts[0], db, tr, um,
								        new ResultCallback<Report.StatusDetails>() {

									        @Override
									        public void onResultReceived(StatusDetails sd) {
										        r.setStatus(sd, true, db, rm, BungeeManager.this);
									        }

								        });
							}
						}

					});
					break;
				case "process":
					getReportAndUserAsynchronously(parts, notify, db, new ReportAndUserResultCallback() {

						@Override
						public void onReportAndUserReceived(Report r, User u) {
							if (r != null && u != null) {
								r.process(u, parts[4], true, parts[3].equals("1"), notify, db);
							}
						}

					});
					break;
				case "process_punish":
					boolean auto = parts[3].equals("1");
					String punishment = message.substring(parts[4].indexOf("/") + 1);
					getReportAndUserAsynchronously(parts, notify, db, new ReportAndUserResultCallback() {

						@Override
						public void onReportAndUserReceived(Report r, User u) {
							if (r != null && u != null) {
								r.processWithPunishment(u, true, auto, punishment, notify, db, vm, BungeeManager.this); // appreciation = "True/punishment", 7 gives index of punishment
							}
						}

					});
					break;
				case "process_abusive":
					boolean autoArchive = parts[3].equals("1");
					long punishSeconds = parts.length >= 6 && parts[5] != null ? Long.parseLong(parts[5])
					        : ReportUtils.getAbusiveReportCooldown();

					getReportAndUserAsynchronously(parts, notify, db, new ReportAndUserResultCallback() {

						@Override
						public void onReportAndUserReceived(Report r, User u) {
							if (r != null && u != null) {
								r.processAbusive(u, true, autoArchive, punishSeconds, notify, db);
							}
						}

					});
					break;
				case "delete":
					// The report is not saved in cache, and is even deleted from cache if cached.
					getReportFromData(message, parts[2], new ResultCallback<Report>() {

						@Override
						public void onResultReceived(Report r) {
							if (r == null) {
								return;
							}

							getUserAsynchronously(parts[0], new ResultCallback<User>() {

								@Override
								public void onResultReceived(User u) {
									r.delete(u, true, db, tr, rm, vm, BungeeManager.this);
								}

							});
						}

					});
					break;
				case "archive":
					getReportAndUserAsynchronously(parts, notify, db, new ReportAndUserResultCallback() {

						@Override
						public void onReportAndUserReceived(Report r, User u) {
							if (r != null) {
								r.archive(u, true, db);
							}
						}

					});
					break;
				case "unarchive":
					getReportAndUserAsynchronously(parts, notify, db, new ReportAndUserResultCallback() {

						@Override
						public void onReportAndUserReceived(Report r, User u) {
							if (r != null) {
								r.unarchive(u, true, db);
							}
						}

					});
					break;

				case "data_changed":
					if (parts.length <= 2) {
						return;
					}

					if (isRecentMsg) {
						// Wait for the database to be updated
						tr.runTaskDelayedly(RECENT_MESSAGE_MAX_DELAY - elapsedTime, new Runnable() {

							@Override
							public void run() {
								updateUsersData(parts, 2);
							}

						});
					} else {
						updateUsersData(parts, 2);
					}

					break;
				case "new_immunity":
					getUserAsynchronously(parts[3], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							if (u != null) {
								if (isRecentMsg) {
									u.setImmunity(parts[0].equals("null") ? null : parts[0].replace("_", " "), true, db,
									        BungeeManager.this, um);
								} else {
									um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
								}
							}
						}

					});
					break;
				case "new_cooldown":
					getUserAsynchronously(parts[3], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							if (u != null) {
								if (isRecentMsg) {
									u.setCooldown(parts[0].equals("null") ? null : parts[0].replace("_", " "), true, db,
									        BungeeManager.this);
								} else {
									um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
								}
							}
						}

					});
					break;
				case "punish":
					getUserAsynchronously(parts[3], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							if (u != null) {
								if (notify) {
									getUserAsynchronously(parts[0], new ResultCallback<User>() {

										@Override
										public void onResultReceived(User staff) {
											u.punish(Long.parseLong(parts[4]), staff, true, db, BungeeManager.this, vm);
										}

									});
								} else {
									um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
								}
							}
						}

					});
					break;
				case "stop_cooldown":
					getUserAsynchronously(parts[3], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							if (u != null) {
								if (notify) {
									getUserAsynchronously(parts[0], new ResultCallback<User>() {

										@Override
										public void onResultReceived(User staff) {
											u.stopCooldown(staff, true, db, BungeeManager.this);
										}

									});
								} else {
									um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
								}
							}
						}

					});
					break;
				case "change_statistic":
					getUserAsynchronously(parts[3], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							if (u != null) {
								if (isRecentMsg) {
									u.changeStatistic(parts[2], Integer.parseInt(parts[0]), true, db,
									        BungeeManager.this);
								} else {
									um.updateDataOfUserWhenPossible(u.getUniqueId(), db, tr);
								}
							}
						}

					});
					break;
				case "tp_loc":
					if (notify) {
						teleportDelayedly(parts[0], MessageUtils.getLocation(parts[2]));
					}
					break;
				case "tp_player":
					if (!notify) {
						break;
					}

					String target = parts[2];
					Player t = Bukkit.getPlayer(target);
					if (t != null) {
						String staff = parts[0];
						sendBungeeMessage("ConnectOther", staff, serverName);
						teleportDelayedly(staff, t.getLocation());
					}
					break;
				case "comment":
					User ru = um.getOnlineUser(parts[3]);
					if (ru == null) {
						return;
					}
					rm.getReportByIdAsynchronously(Integer.parseInt(parts[0]), false, notify, false, db, tr, um,
					        new ResultCallback<Report>() {

						        @Override
						        public void onResultReceived(Report r) {
							        if (r != null) {
								        r.getCommentByIdAsynchronously(Integer.parseInt(parts[2]), db, tr, um,
								                new ResultCallback<Comment>() {

									                @Override
									                public void onResultReceived(Comment c) {
										                ru.sendCommentNotification(r, c, true, db, vm,
										                        BungeeManager.this);
									                }

								                });
							        }
						        }
					        });
					break;
				case "player_status":
					String name = parts[0];
					boolean online = Boolean.parseBoolean(parts[2]);
					if (!online && Bukkit.getPlayer(name) != null) {
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

	private interface ReportAndUserResultCallback {
		void onReportAndUserReceived(Report r, User u);
	}

	private void getReportAndUserAsynchronously(String[] parts, boolean notify, Database db,
	        ReportAndUserResultCallback resultCallback) {
		getReportAsynchronously(parts[2], notify, db, new ResultCallback<Report>() {

			@Override
			public void onResultReceived(Report r) {
				if (notify) {
					getUserAsynchronously(parts[0], new ResultCallback<User>() {

						@Override
						public void onResultReceived(User u) {
							resultCallback.onReportAndUserReceived(r, u);
						}

					});
				} else {
					resultCallback.onReportAndUserReceived(r, null);
				}
			}

		});
	}

	private void getReportAsynchronously(String reportId, boolean useCache, Database db,
	        ResultCallback<Report> resultCallback) {
		try {
			getReportAsynchronously(Integer.parseInt(reportId), useCache, db, resultCallback);
		} catch (NumberFormatException ignored) {}
	}

	private void getReportAsynchronously(int reportId, boolean useCache, Database db,
	        ResultCallback<Report> resultCallback) {
		rm.getReportByIdAsynchronously(reportId, false, useCache, false, db, tr, um, resultCallback);
	}

	private void getUserAsynchronously(String uuid, ResultCallback<User> resultCallback) {
		um.getUserAsynchronously(uuid, db, tr, resultCallback);
	}

	private void getReportFromData(String message, String firstPart, ResultCallback<Report> resultCallback) {
		int reportDataStartIndex = message.indexOf(firstPart);
		if (reportDataStartIndex < 0) {
			resultCallback.onResultReceived(null);
			return;
		}
		String reportDataAsString = message.substring(reportDataStartIndex);
		Map<String, Object> reportData = Report.parseBasicDataFromString(reportDataAsString);
		if (reportData == null) {
			resultCallback.onResultReceived(null);
			return;
		}

		Report.asynchronouslyFrom(reportData, false, db, tr, um, resultCallback);
	}

	private String getReportDataAsString(String message, int reportDataStartIndex) {
		if (reportDataStartIndex < 0) {
			return null;
		}

		return message.substring(reportDataStartIndex);
	}

	private void processNewReportMessage(boolean isRecentMsg, boolean notify, String reportServer,
	        boolean reportMissingData, String reportDataAsString) {
		Map<String, Object> reportData = Report.parseBasicDataFromString(reportDataAsString);
		if (reportData == null) {
			return;
		}

		int reportId = (int) reportData.get("report_id");

		if (isRecentMsg) {
			rm.updateAndGetReport(reportId, reportData, false, false, db, tr, um, createNewReportResultCallback(notify,
			        reportServer, reportMissingData, reportDataAsString, reportData));
		} else if (notify) {
			getReportAsynchronously(reportId, false, db, createNewReportResultCallback(notify, reportServer,
			        reportMissingData, reportDataAsString, reportData));
		} else {
			getReportAsynchronously(reportId, false, db, new ResultCallback<Report>() {

				@Override
				public void onResultReceived(Report r) {
					if (r != null) {
						ReportUtils.sendReport(r, reportServer, notify, db, vm, BungeeManager.this);
					}
				}

			});
		}

	}

	private ResultCallback<Report> createNewReportResultCallback(boolean notify, String reportServer,
	        boolean reportMissingData, String reportDataAsString, Map<String, Object> reportData) {
		return new ResultCallback<Report>() {

			@Override
			public void onResultReceived(Report r) {
				if (r != null && r.getBasicDataAsString().equals(reportDataAsString)) {
					sendReportAndImplementMissingData(r, reportServer, notify, reportMissingData);
				} else {
					sendReportWithReportData(reportData, reportServer, notify, reportMissingData);
				}
			}

		};
	}

	private void sendReportWithReportData(Map<String, Object> reportData, String reportServer, boolean notify,
	        boolean reportMissingData) {
		Report.asynchronouslyFrom(reportData, false, db, tr, um, new ResultCallback<Report>() {

			@Override
			public void onResultReceived(Report r) {
				if (r != null) {
					sendReportAndImplementMissingData(r, reportServer, notify, reportMissingData);
				}
			}

		});
	}

	private void sendReportAndImplementMissingData(Report r, String reportServer, boolean notify,
	        boolean reportMissingData) {
		ReportUtils.sendReport(r, reportServer, notify, db, vm, BungeeManager.this);
		if (reportMissingData) {
			implementMissingData(r, db);
		}
	}

	private void updateUsersData(String[] parts, int uuidStartIndex) {
		List<UUID> usersUUID = new ArrayList<>();
		for (int i = uuidStartIndex; i < parts.length; i++) {
			try {
				usersUUID.add(UUID.fromString(parts[i]));
			} catch (IllegalArgumentException ignored) {}
		}
		um.updateDataOfUsersWhenPossible(usersUUID, db, tr);
	}

	private void teleportDelayedly(String name, Location loc) {
		tr.runTaskDelayedly(1000L, new Runnable() {

			@Override
			public void run() {
				Player p = Bukkit.getPlayer(name);
				if (p != null) {
					p.teleport(loc);
					ConfigSound.TELEPORT.play(p);
				}
			}

		});
	}

	private void implementMissingData(Report r, Database db) {
		Map<String, Object> reportData = new HashMap<>();
		if (ReportUtils.collectAndFillReportedData(r.getReported(), this, reportData)) {
			StringBuilder queryArguments = new StringBuilder();
			List<Object> queryParams = new ArrayList<>();
			for (Entry<String, Object> data : reportData.entrySet()) {
				if (queryArguments.length() > 0) {
					queryArguments.append(",");
				}
				queryArguments.append("'").append(data.getKey()).append("'=?");
				queryParams.add(data.getValue());
			}
			queryParams.add(r.getId());

			String query = "UPDATE tigerreports_reports SET " + queryArguments + " WHERE report_id=?";
			db.updateAsynchronously(query, queryParams);
		}
	}

	public void collectOnlinePlayers() {
		onlinePlayersCollected = false;
		sendBungeeMessage("PlayerList", "ALL");
	}

	public boolean isOnline(String name) {
		return onlinePlayers.contains(name);
	}

	public List<String> getOnlinePlayers() {
		return onlinePlayersCollected ? new ArrayList<>(onlinePlayers) : null;
	}

	private void setPlayerStatus(String name, boolean online) {
		if (online) {
			if (!onlinePlayers.contains(name)) {
				onlinePlayers.add(name);
			}
		} else {
			onlinePlayers.remove(name);
		}
	}

	public void updatePlayerStatus(String name, boolean online) {
		if (!initialized) {
			return;
		}

		setPlayerStatus(name, online);
		sendPluginNotificationToAll(name + " player_status " + online);
	}

	public void destroy() {
		Messenger messenger = tr.getServer().getMessenger();
		messenger.unregisterOutgoingPluginChannel(tr, "BungeeCord");
		messenger.unregisterIncomingPluginChannel(tr, "BungeeCord", this);
	}

}
