package fr.mrtigreroux.tigerreports;

import java.lang.ref.WeakReference;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import fr.mrtigreroux.tigerreports.bungee.BungeeManager;
import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.constants.MenuItem;
import fr.mrtigreroux.tigerreports.data.constants.MenuRawItem;
import fr.mrtigreroux.tigerreports.data.database.Database;
import fr.mrtigreroux.tigerreports.data.database.MySQL;
import fr.mrtigreroux.tigerreports.data.database.SQLite;
import fr.mrtigreroux.tigerreports.listeners.InventoryListener;
import fr.mrtigreroux.tigerreports.listeners.PlayerListener;
import fr.mrtigreroux.tigerreports.logs.Logger;
import fr.mrtigreroux.tigerreports.managers.ReportsManager;
import fr.mrtigreroux.tigerreports.managers.UpdatesManager;
import fr.mrtigreroux.tigerreports.managers.UsersManager;
import fr.mrtigreroux.tigerreports.managers.VaultManager;
import fr.mrtigreroux.tigerreports.objects.users.User;
import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;
import fr.mrtigreroux.tigerreports.tasks.runnables.MenuUpdater;
import fr.mrtigreroux.tigerreports.tasks.runnables.ReportsNotifier;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.FileUtils;
import fr.mrtigreroux.tigerreports.utils.LogUtils;
import fr.mrtigreroux.tigerreports.utils.WebUtils;

/**
 * @author MrTigreroux Published on: 30/06/2016
 */
public class TigerReports extends JavaPlugin implements TaskScheduler {

	private static final String SPIGOTMC_RESOURCE_ID = "25773";

	private static TigerReports instance;

	private boolean loaded = false;
	private String newVersion = null;
	private boolean needUpdatesInstructions = false;
	private WeakReference<Database> database;
	private BungeeManager bungeeManager;
	private UsersManager usersManager;
	private ReportsManager reportsManager;
	private VaultManager vaultManager = null;
	private final Set<Consumer<Boolean>> loadUnloadListeners = new HashSet<>();

	public TigerReports() {}

	@Override
	public void onEnable() {
		instance = this;
		MenuRawItem.init();
		load();

		PluginDescriptionFile desc = getDescription();
		if (!desc.getName().equals("TigerReports") || desc.getAuthors().size() != 1
		        || !desc.getAuthors().contains("MrTigreroux")) {
			Logger.CONFIG.error(ConfigUtils.getInfoMessage("The file plugin.yml has been edited without authorization.",
			        "Le fichier plugin.yml a ete modifie sans autorisation."));
			Bukkit.shutdown();
			return;
		}
	}

	public void load() {
		Logger.MAIN.info(() -> "load()");
		for (ConfigFile configFiles : ConfigFile.values()) {
			configFiles.load(this);
		}

		MenuItem.init();

		usersManager = new UsersManager();
		reportsManager = new ReportsManager();

		PluginManager pm = Bukkit.getServer().getPluginManager();
		vaultManager = new VaultManager(pm.getPlugin("Vault") != null);

		initializeDatabase(new DatabaseInitializationCallback() {

			@Override
			public void onDatabaseInitializationDone(Database db) {
				if (bungeeManager == null) {
					bungeeManager = new BungeeManager(instance, reportsManager, db, vaultManager, usersManager);
				}
				bungeeManager.startSetupCommunicationSession(true);

				pm.registerEvents(new InventoryListener(db, usersManager), instance);
				pm.registerEvents(
				        new PlayerListener(reportsManager, db, instance, bungeeManager, vaultManager, usersManager),
				        instance);

				setCommandExecutor("report",
				        new ReportCommand(instance, reportsManager, db, bungeeManager, vaultManager, usersManager));
				setCommandExecutor("reports",
				        new ReportsCommand(reportsManager, db, instance, bungeeManager, vaultManager, usersManager));

				ReportsNotifier.startIfNeeded(db, instance);

				for (Player p : Bukkit.getOnlinePlayers()) {
					usersManager.processUserConnection(p);
					User u = usersManager.getOnlineUser(p);
					if (u != null) {
						u.updateBasicData(db, bungeeManager, usersManager);
						bungeeManager.processPlayerConnection(p);
					} else {
						LogUtils.logUnexpectedOfflineUser(Logger.MAIN, "load()", p);
					}
				}

				setLoaded(true);

				WebUtils.checkNewVersion(instance, instance, SPIGOTMC_RESOURCE_ID, new ResultCallback<String>() {

					@Override
					public void onResultReceived(String newVersion) {
						instance.newVersion = newVersion;
					}

				});

				updateNeedUpdatesInstructions(true);
			}

		});
	}

	private void setCommandExecutor(String commandName, CommandExecutor commandExecutor) {
		PluginCommand command = getCommand(commandName);
		if (command != null) {
			command.setExecutor(commandExecutor);
		} else {
			Logger.CONFIG.error(ConfigUtils.getInfoMessage("Command /" + commandName + " is not registered.",
			        "La commande /" + commandName + " n'est pas definie."));
		}
	}

	public static TigerReports getInstance() {
		return instance;
	}

	public String getNewVersion() {
		return newVersion;
	}

	public Database getDatabase(boolean initializeIfNecessary) {
		if (initializeIfNecessary && (database == null || database.get() == null)) {
			initializeDatabase(null);
		}
		return database != null ? database.get() : null;
	}

	public BungeeManager getBungeeManager() {
		return bungeeManager;
	}

	public UsersManager getUsersManager() {
		return usersManager;
	}

	public ReportsManager getReportsManager() {
		return reportsManager;
	}

	public VaultManager getVaultManager() {
		return vaultManager;
	}

	private interface DatabaseInitializationCallback {
		void onDatabaseInitializationDone(Database db);
	}

	public void initializeDatabase(DatabaseInitializationCallback initializationCallback) {
		runTaskAsynchronously(new Runnable() {

			@Override
			public void run() {
				Database database;
				try {
					FileConfiguration config = ConfigFile.CONFIG.get();
					MySQL mysql = new MySQL(config.getString("MySQL.Custom.DriverClassName"),
					        config.getString("MySQL.Custom.ConnectionUrl"), config.getString("MySQL.Host"),
					        config.getInt("MySQL.Port"), config.getString("MySQL.Database"),
					        config.getString("MySQL.Username"), config.getString("MySQL.Password"),
					        ConfigUtils.isEnabled(config, "MySQL.UseSSL"),
					        ConfigUtils.isEnabled(config, "MySQL.VerifyServerCertificate"), instance);
					mysql.check();
					database = mysql;
					Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage("The plugin is using a MySQL database.",
					        "Le plugin utilise une base de donnees MySQL."));
				} catch (Exception invalidMySQL) {
					database = new SQLite(instance, getDataFolder(), "tigerreports.db");
					Logger.CONFIG.info(() -> ConfigUtils.getInfoMessage(
					        "The plugin is using the SQLite (default) database (because the MySQL settings are invalid: "
					                + invalidMySQL.getMessage() + ").",
					        "Le plugin utilise la base de donnees SQLite (par defaut, car les parametres MySQL ne sont pas valides: "
					                + invalidMySQL.getMessage() + ")."));
					Logger.CONFIG.debug(() -> "Exception during MySQL connection attempt: ", invalidMySQL);
				}
				final Database db = database;
				db.initialize();

				runTask(new Runnable() {

					@Override
					public void run() {
						instance.database = new WeakReference<>(db);
						if (initializationCallback != null) {
							initializationCallback.onDatabaseInitializationDone(db);
						}
					}

				});
			}

		});
	}

	@Override
	public int runTaskDelayedly(long delay, Runnable task) {
		return Bukkit.getScheduler().runTaskLater(this, task, msToTicks(delay)).getTaskId();
	}

	@Override
	public int runTaskDelayedlyAsynchronously(long delay, Runnable task) {
		return Bukkit.getScheduler().runTaskLaterAsynchronously(this, task, msToTicks(delay)).getTaskId();
	}

	/**
	 * Conversion of ms to ticks.
	 * 
	 * @param ms
	 * @return
	 */
	private static long msToTicks(long ms) {
		return (ms * 20) / 1000;
	}

	@Override
	public void runTaskAsynchronously(Runnable task) {
		Bukkit.getScheduler().runTaskAsynchronously(this, task);
	}

	@Override
	public void runTask(Runnable task) {
		Bukkit.getScheduler().runTask(this, task);
	}

	@Override
	public int runTaskRepeatedly(long delay, long period, Runnable task) {
		return Bukkit.getScheduler().runTaskTimer(this, task, msToTicks(delay), msToTicks(period)).getTaskId();
	}

	@Override
	public void cancelTask(int taskId) {
		Bukkit.getScheduler().cancelTask(taskId);
	}

	@Override
	public void onDisable() {
		unload();
	}

	public void unload() {
		Logger.MAIN.info(() -> "unload()");
		MenuUpdater.stop(this);
		ReportsNotifier.stop(this);

		setCommandExecutor("report", null);
		setCommandExecutor("reports", null);

		if (usersManager != null) {
			Collection<User> users = usersManager.getUsers();
			if (users != null && !users.isEmpty()) {
				for (User u : users) {
					if (u.getOpenedMenu() != null) {
						u.getPlayer().closeInventory();
					}
				}
			}
		}

		HandlerList.unregisterAll(this); // Unregister all event listeners.

		reportsManager = null;
		usersManager = null;

		if (bungeeManager != null) {
			bungeeManager.destroy();
			bungeeManager = null;
		}

		vaultManager = null;

		Database db = getDatabase(false);
		if (db != null) {
			db.closeConnection();
		}

		setLoaded(false);
	}

	public void addAndNotifyLoadUnloadListener(Consumer<Boolean> listener) {
		Logger.MAIN.info(() -> "addAndNotifyLoadUnloadListener(" + listener + ")");
		loadUnloadListeners.add(listener);
		listener.accept(loaded);
	}

	public void removeLoadUnloadListener(Consumer<Boolean> listener) {
		Logger.MAIN.info(() -> "removeLoadUnloadListener(" + listener + ")");
		loadUnloadListeners.remove(listener);
	}

	private void setLoaded(boolean loaded) {
		Logger.MAIN.info(() -> "setLoaded(" + loaded + ")");
		if (this.loaded != loaded) {
			this.loaded = loaded;
			broadcastLoadUnload(this.loaded);
		}
	}

	private void broadcastLoadUnload(boolean loaded) {
		Logger.MAIN.info(() -> "broadcastLoadUnload(" + loaded + ")");
		for (Consumer<Boolean> listeners : loadUnloadListeners) {
			listeners.accept(loaded);
		}
	}

	public void updateNeedUpdatesInstructions(boolean updateLastVersionUsed) {
		needUpdatesInstructions = UpdatesManager.needUpdatesInstructions(instance);
		if (!needUpdatesInstructions) {
			if (updateLastVersionUsed) {
				UpdatesManager.updateLastVersionUsed(instance);
			}
		} else {
			String version = getDescription().getVersion();
			String oldVersion = UpdatesManager.getLastVersionUsed(instance);
			String oldVersionStr;
			boolean english = ConfigUtils.getInfoLanguage().equalsIgnoreCase("English");
			if (UpdatesManager.DEFAULT_LAST_USED_VERSION.equals(oldVersion)) {
				FileTime pluginFolderCreationTime = FileUtils.getFileCreationTime(getDataFolder().toPath());
				if (pluginFolderCreationTime != null
				        && pluginFolderCreationTime.toInstant().until(Instant.now(), ChronoUnit.DAYS) <= 7) {
					// Consider it is a fresh new installation.
					UpdatesManager.updateLastVersionUsed(instance);
					needUpdatesInstructions = false;
					return;
				}
				oldVersionStr = english ? "unknown" : "inconnue";
			} else {
				oldVersionStr = oldVersion;
			}
			String command = "/reports update_data " + oldVersion;
			Logger.CONFIG.error(english ? "You updated the plugin from an older version (" + oldVersionStr
			        + ") to the current version (" + version
			        + ") and some data (database and config files) need to be updated. Please make a backup of your data of TigerReports plugin, then run the command: "
			        + command
			        : "Vous avez mis a jour le plugin depuis une ancienne version (" + oldVersionStr
			                + ") vers la version actuelle (" + version
			                + ") et certaines donnees (base de donnees et fichiers de configuration) doivent etre mises a jour. Veuillez faire une sauvegarde de vos donnees du plugin TigerReports, puis executer la commande: "
			                + command);
		}
	}

	public boolean needUpdatesInstructions() {
		return needUpdatesInstructions;
	}

}
