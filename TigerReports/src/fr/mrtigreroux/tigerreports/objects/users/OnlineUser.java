package fr.mrtigreroux.tigerreports.objects.users;

import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.data.config.ConfigFile;
import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.data.constants.Status;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.menus.*;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class OnlineUser extends User {

	private final Player p;
	private Menu openedMenu = null;
	private Comment editingComment = null;
	private boolean notifications = true;

	public OnlineUser(Player p) {
		super(p.getUniqueId().toString());
		this.p = p;
		this.name = p.getName();
	}

	public Player getPlayer() {
		return p;
	}

	@Override
	public String getName() {
		if (name == null)
			name = p.getName();
		return name;
	}

	public String getDisplayName() {
		if (displayName == null)
			displayName = TigerReports.getInstance().getVaultManager().getOnlinePlayerDisplayName(p);
		return displayName;
	}

	public void openReasonMenu(int page, User tu) {
		new ReasonMenu(this, page, tu).open(true);
	}

	public void openDelayedlyReportsMenu() {
		p.closeInventory();
		Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				openReportsMenu(1, false);
			}

		}, 10);
	}

	public void openReportsMenu(int page, boolean sound) {
		new ReportsMenu(this, page).open(sound);
	}

	public void openReportMenu(int reportId) {
		new ReportMenu(this, reportId).open(true);
	}

	public void openReportMenu(Report r) {
		ReportMenu rm = new ReportMenu(this, r.getId());
		rm.setReport(r);
		rm.open(true);
	}

	public void openProcessMenu(Report r) {
		new ProcessMenu(this, r.getId()).open(true);
	}

	public void openPunishmentMenu(int page, Report r) {
		String command = ConfigFile.CONFIG.get().getString("Config.Punishments.PunishmentsCommand");
		if (command != null && !command.equalsIgnoreCase("none")) {
			r.process(uuid, "True", false, Permission.STAFF_ARCHIVE_AUTO.isOwned(this), true);

			Bukkit.dispatchCommand(p,
			        command.replace("_Reported_", r.getPlayerName("Reported", false, false))
			                .replace("_Staff_", name)
			                .replace("_Id_", Integer.toString(r.getId()))
			                .replace("_Reason_", r.getReason(false))
			                .replace("_Reporter_", r.getPlayerName("Reporter", false, false)));
		}
		new PunishmentMenu(this, page, r.getId()).open(true);
	}

	public void openConfirmationMenu(Report r, String action) {
		new ConfirmationMenu(this, r.getId(), action).open(true);
	}

	public void openDelayedlyCommentsMenu(Report r) {
		Bukkit.getScheduler().runTaskLater(TigerReports.getInstance(), new Runnable() {

			@Override
			public void run() {
				openCommentsMenu(1, r);
			}

		}, 10);
	}

	public void openCommentsMenu(int page, Report r) {
		new CommentsMenu(this, page, r.getId()).open(true);
	}

	public void openArchivedReportsMenu(int page, boolean sound) {
		new ArchivedReportsMenu(this, page).open(sound);
	}

	public void openUserMenu(User tu) {
		new UserMenu(this, tu).open(true);
	}

	public void openUserReportsMenu(User tu, int page) {
		new UserReportsMenu(this, page, tu).open(true);
	}

	public void openUserArchivedReportsMenu(User tu, int page) {
		new UserArchivedReportsMenu(this, page, tu).open(true);
	}

	public void openUserAgainstReportsMenu(User tu, int page) {
		new UserAgainstReportsMenu(this, page, tu).open(true);
	}

	public void openUserAgainstArchivedReportsMenu(User tu, int page) {
		new UserAgainstArchivedReportsMenu(this, page, tu).open(true);
	}

	public void setOpenedMenu(Menu menu) {
		openedMenu = menu;
	}

	public Menu getOpenedMenu() {
		return openedMenu;
	}

	public void setCooldown(String cooldown) {
		this.cooldown = cooldown;
	}

	public void updateLastMessages(String newMessage) {
		int lastMessagesAmount = ConfigFile.CONFIG.get().getInt("Config.MessagesHistory", 5);
		if (lastMessagesAmount <= 0)
			return;

		if (lastMessages.size() >= lastMessagesAmount)
			lastMessages.remove(0);
		lastMessages.add(MessageUtils.getNowDate() + ":" + newMessage);
	}

	@Override
	public void sendMessage(Object message) {
		if (message instanceof TextComponent) {
			p.spigot().sendMessage((TextComponent) message);
		} else {
			p.sendMessage((String) message);
		}
	}

	public void printInChat(Report r, String[] lines) {
		String reportName = r.getName();
		for (String line : lines)
			sendMessage(MessageUtils.getAdvancedMessage(line, "_ReportButton_",
			        Message.REPORT_BUTTON.get().replace("_Report_", reportName),
			        Message.ALERT_DETAILS.get().replace("_Report_", reportName),
			        "/tigerreports:reports #" + r.getId()));
		ConfigSound.MENU.play(p);
		p.closeInventory();
	}

	public void sendMessageWithReportButton(String message, Report r) {
		String reportName = r.getName();
		sendMessage(MessageUtils.getAdvancedMessage(message, "_ReportButton_",
		        Message.REPORT_BUTTON.get().replace("_Report_", reportName),
		        Message.ALERT_DETAILS.get().replace("_Report_", reportName), "/tigerreports:reports #" + r.getId()));
	}

	public void setStaffNotifications(boolean state) {
		notifications = state;
	}

	public boolean acceptsNotifications() {
		return notifications;
	}

	public void sendNotifications() {
		TigerReports tr = TigerReports.getInstance();
		Bukkit.getScheduler().runTaskAsynchronously(tr, new Runnable() {

			@Override
			public void run() {
				for (String notification : getNotifications()) {
					if (notification != null) {
						if (notification.contains(":")) {
							String[] parts = notification.split(":");
							try {
								Report r = tr.getReportsManager().getReportById(Integer.parseInt(parts[0]), false);
								Comment c = r.getCommentById(Integer.parseInt(parts[1]));
								Bukkit.getScheduler().runTask(tr, new Runnable() {

									@Override
									public void run() {
										sendCommentNotification(r, c, false);
									}

								});
							} catch (Exception invalidNotification) {}
						} else if (ConfigUtils.playersNotifications()) {
							try {
								Report r = tr.getReportsManager().getReportById(Integer.parseInt(notification), false);
								Bukkit.getScheduler().runTask(tr, new Runnable() {

									@Override
									public void run() {
										sendReportNotification(r, false);
									}

								});
							} catch (Exception invalidNotification) {}

						}
					}
				}
				setNotifications(null);
			}

		});

	}

	public void sendCommentNotification(Report r, Comment c, boolean direct) {
		if (!direct && !c.getStatus(true).equals("Sent"))
			return;
		p.sendMessage(Message.COMMENT_NOTIFICATION.get()
		        .replace("_Player_", c.getAuthorDisplayName())
		        .replace("_Reported_", r.getPlayerName("Reported", false, true))
		        .replace("_Time_", MessageUtils.getTimeAgo(r.getDate()))
		        .replace("_Message_", c.getMessage()));
		c.setStatus("Read " + MessageUtils.getNowDate());
	}

	public void sendReportNotification(Report r, boolean direct) {
		if (!direct && r.getStatus() != Status.DONE)
			return;

		sendMessage(
		        MessageUtils
		                .getAdvancedMessage(
		                        Message.REPORT_NOTIFICATION.get()
		                                .replace("_Player_", r.getProcessor())
		                                .replace("_Appreciation_",
		                                        Message.valueOf(r.getAppreciation(true).toUpperCase()).get())
		                                .replace("_Time_", MessageUtils.getTimeAgo(r.getDate())),
		                        "_Report_", r.getName(), r.getText(), null));
	}

	public void createComment(Report r) {
		editComment(new Comment(r, null, null, null, null, null));
	}

	public void cancelComment() {
		if (editingComment == null)
			return;
		Report r = editingComment.getReport();
		sendMessageWithReportButton(Message.CANCEL_COMMENT.get().replace("_Report_", r.getName()), r);
		editingComment = null;
	}

	public void editComment(Comment c) {
		if (c.getAuthorUniqueId() != null && !c.getAuthorUniqueId().equalsIgnoreCase(p.getUniqueId().toString())) {
			ConfigSound.ERROR.play(p);
			return;
		}
		editingComment = c;
		p.closeInventory();
		String reportName = c.getReport().getName();
		sendMessage(MessageUtils.getAdvancedMessage(Message.EDIT_COMMENT.get().replace("_Report_", reportName),
		        "_CancelButton_", Message.CANCEL_BUTTON.get(),
		        Message.CANCEL_BUTTON_DETAILS.get().replace("_Report_", reportName),
		        "/tigerreports:reports canceledit"));
	}

	public void setEditingComment(Comment c) {
		editingComment = c;
	}

	public Comment getEditingComment() {
		return editingComment;
	}

	public boolean canArchive(Report r) {
		return Permission.STAFF_ARCHIVE.isOwned(this)
		        && (r.getStatus() == Status.DONE || !ReportUtils.onlyDoneArchives());
	}

}
