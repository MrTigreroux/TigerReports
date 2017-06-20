package fr.mrtigreroux.tigerreports.objects.users;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.config.ConfigSound;
import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.data.constants.Permission;
import fr.mrtigreroux.tigerreports.objects.Comment;
import fr.mrtigreroux.tigerreports.objects.Report;
import fr.mrtigreroux.tigerreports.objects.menus.ArchivedReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ProcessMenu;
import fr.mrtigreroux.tigerreports.objects.menus.CommentsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ConfirmationMenu;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.menus.ReasonMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserMenu;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReflectionUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;

/**
 * @author MrTigreroux
 */

public class OnlineUser extends User {
	
	private Player p;
	private Menu openedMenu = null;
	private Report commentingReport = null;
	private Comment modifiedComment = null;
	private Material signMaterial = null;
	private Byte signData = null;
	private String lastMessages = null;
	private boolean notifications = true;
	
	public OnlineUser(Player p) {
		super(p.getUniqueId().toString());
		this.p = p;
	}
	
	public Player getPlayer() {
		return p;
	}

	public void playSound(ConfigSound sound) {
		if(sound != null) p.playSound(p.getLocation(), sound.get(), 1, 1);
	}

	public void openReasonMenu(int page, User tu) {
		new ReasonMenu(this, page, tu).open(true);
	}

	public void openReportsMenu(int page, boolean sound) {
		new ReportsMenu(this, page).open(sound);
	}
	
	public void openReportMenu(Report r) {
		new ReportMenu(this, r.getId()).open(true);
	}

	public void openAppreciationMenu(Report r) {
		new ProcessMenu(this, r.getId()).open(true);
	}
	
	public void openConfirmationMenu(Report r, String action) {
		new ConfirmationMenu(this, r.getId(), action).open(true);
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
	
	public void setOpenedMenu(Menu menu) {
		openedMenu = menu;
		save();
	}

	public Menu getOpenedMenu() {
		return openedMenu;
	}
	
	public void setCooldown(String cooldown) {
		this.cooldown = cooldown;
		save();
	}
	
	public boolean t(Permission permission) {
		return permission == null ? true : p.hasPermission(permission.get());
	}
	
	public void updateLastMessages(String newMessage) {
		int lastMessagesAmount = ConfigUtils.getMessagesHistory();
		if(lastMessagesAmount <= 0) return;
		
		ArrayList<String> lastMessagesList = new ArrayList<String>();
		if(lastMessages != null) lastMessagesList = new ArrayList<String>(Arrays.asList(lastMessages.split("#next#")));
		if(lastMessagesList.size() >= lastMessagesAmount) lastMessagesList.remove(0);
		lastMessagesList.add(newMessage);
		this.lastMessages = String.join("#next#", lastMessagesList);
		save();
	}
	
	public String getLastMessages() {
		return lastMessages;
	}
	
	@Override
	public void sendMessage(Object message) {
		if(message instanceof TextComponent) p.spigot().sendMessage((TextComponent) message);
		else p.sendMessage((String) message);
	}
	
	public void printInChat(Report r, String[] lines) {
		String reportName = r.getName();
		for(String line : lines) sendMessage(MessageUtils.getAdvancedMessage(line, "_ReportButton_", Message.REPORT_BUTTON.get().replace("_Report_", reportName), Message.ALERT_DETAILS.get().replace("_Report_", reportName), "/reports #"+r.getId()));
		playSound(ConfigSound.MENU);
		p.closeInventory();
	}
	
	public void setStaffNotifications(boolean state) {
		notifications = state;
		save();
	}
	
	public boolean acceptsNotifications() {
		return notifications;
	}
	
	public void sendNotification(String comment, boolean direct) {
		try {
			String[] parts = comment.split(":");
			Report r = ReportUtils.getReportById(Integer.parseInt(parts[0].replace("Report", "")));
			Comment c = r.getComments().get(Integer.parseInt(parts[1].replace("Comment", "")));
			if(!direct && !c.getStatus(true).equals("Sent")) return;
			p.sendMessage(Message.COMMENT_NOTIFICATION.get().replace("_Player_", c.getAuthor())
					.replace("_Reported_", r.getPlayerName("Reported", false)).replace("_Time_", MessageUtils.convertToSentence(MessageUtils.getSeconds(MessageUtils.getNowDate())-MessageUtils.getSeconds(r.getDate())))
					.replace("_Message_",c.getMessage()));
			List<String> notifications = getNotifications();
			notifications.remove(comment);
			setNotifications(notifications);
			c.setStatus("Read");
		} catch (Exception invalidNotification) {}
	}

	@SuppressWarnings("deprecation")
	public void comment(Report r) {
		try {
			Location loc = p.getLocation();
			World world = loc.getWorld();
			Block b = world.getBlockAt(loc.getBlockX(), world.getMaxHeight()-1, loc.getBlockZ());
			signMaterial = b.getType();
			signData = b.getData();
			
			Block support = b.getRelative(BlockFace.DOWN);
			if(support.getType() == Material.AIR) support.setType(Material.BEDROCK);
			b.setType(Material.SIGN_POST);
			Sign s = (Sign) b.getState();
			s.setLine(0, "§7[§6TigerReports§7]");
			s.setLine(1, "§e"+p.getName());
			s.setLine(2, "§8rédige un");
			s.setLine(3, "§8commentaire");
			s.update();
			
			setCommentingReport(r);
			save();
			Object tileEntity = ReflectionUtils.getDeclaredField(s,  "sign");
			ReflectionUtils.setDeclaredField(tileEntity, "isEditable", true);
			ReflectionUtils.setDeclaredField(tileEntity, "h", ReflectionUtils.getHandle(p));
			ReflectionUtils.sendPacket(p,  ReflectionUtils.getPacket("PacketPlayOutOpenSignEditor", ReflectionUtils.callDeclaredConstructor(ReflectionUtils.getNMSClass("BlockPosition"), s.getX(), s.getY(), s.getZ())));
		} catch (Exception error) {}
	}
	
	@SuppressWarnings("deprecation")
	public void updateSignBlock(Block b) {
		b.setType(signMaterial != null ? signMaterial : Material.AIR);
		Block support = b.getRelative(BlockFace.DOWN);
		if(support.getType() == Material.BEDROCK) support.setType(Material.AIR);
		if(signData != null) b.setData(signData);
		signMaterial = null;
		signData = null;
		save();
	}
	
	public void setCommentingReport(Report r) {
		this.commentingReport = r;
	}
	
	public Report getCommentingReport() {
		return commentingReport;
	}
	
	public void setModifiedComment(Comment c) {
		modifiedComment = c;
	}
	
	public Comment getModifiedComment() {
		return modifiedComment;
	}
	
}
