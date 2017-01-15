package fr.mrtigreroux.tigerreports.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.ConfigFile;
import fr.mrtigreroux.tigerreports.data.ConfigSound;
import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
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
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class User {
	
	private Player p;
	private UUID uuid;
	private Menu openedMenu = null;
	private int commentingReport = -1;
	private int modifiedComment = -1;
	private Material signMaterial = null;
	private Byte signData = null;
	private String lastMessages = null;
	private boolean notifications = true;
	
	public User(Player p) {
		this.p = p;
		this.uuid = p.getUniqueId();
	}
	
	public Player getPlayer() {
		return p;
	}

	public void playSound(Sound sound) {
		if(sound != null) p.playSound(p.getLocation(), sound, 1, 1);
	}

	public void openReasonMenu(int page, String target) {
		new ReasonMenu(this, page, target).open(true);
	}

	public void openReportsMenu(int page, boolean sound) {
		new ReportsMenu(this, page).open(sound);
	}
	
	public void openReportMenu(Report r) {
		new ReportMenu(this, r).open(true);
	}

	public void openAppreciationMenu(Report r) {
		new ProcessMenu(this, r).open(true);
	}
	
	public void openConfirmationMenu(Report r, String action) {
		new ConfirmationMenu(this, r, action).open(true);
	}

	public void openCommentsMenu(int page, Report r) {
		new CommentsMenu(this, page, r).open(true);
	}

	public void openUserMenu(String target) {
		new UserMenu(this, target).open(true);
	}
	
	public void setOpenedMenu(Menu menu) {
		openedMenu = menu;
		save();
	}

	public Menu getOpenedMenu() {
		return openedMenu;
	}
	
	public boolean hasPermission(Permission permission) {
		return p.hasPermission(permission.get());
	}
	
	public void updateLastMessages(String newMessage) {
		int lastMessagesNumber = ConfigUtils.getMessagesHistory();
		if(lastMessagesNumber <= 0) return;
		
		ArrayList<String> lastMessagesList = new ArrayList<String>();
		if(lastMessages != null) lastMessagesList = new ArrayList<String>(Arrays.asList(lastMessages.split("#next#")));
		if(lastMessagesList.size() >= lastMessagesNumber) lastMessagesList.remove(0);
		lastMessagesList.add(newMessage);
		this.lastMessages = String.join("#next#", lastMessagesList);
		save();
	}
	
	public String getLastMessages() {
		return lastMessages;
	}
	
	public void sendMessage(Object message) {
		if(message instanceof TextComponent) p.spigot().sendMessage((TextComponent) message);
		else p.sendMessage((String) message);
	}
	
	public void printInChat(Report r, String[] lines) {
		String reportName = r.getName();
		for(String line : lines) sendMessage(MessageUtils.getAdvancedMessage(line, "_ReportButton_", Message.REPORT_BUTTON.get().replace("_Report_", reportName), Message.ALERT_DETAILS.get().replace("_Report_", reportName), "/reports #"+r.getNumber()));
		playSound(ConfigSound.MENU.get());
		p.closeInventory();
	}
	
	public boolean acceptsNotifications() {
		return notifications;
	}
	
	public void setNotifications(boolean state) {
		notifications = state;
		save();
	}
	
	public void sendNotification(String comment) {
		try {
			Report r = new Report(Integer.parseInt(comment.split(":")[0].replaceFirst("Report#", "")));
			String commentPath = r.getConfigPath()+".Comments.Comment"+comment.split(":")[1].replaceFirst("Comment#", "");
			if(!ConfigFile.REPORTS.get().getString(commentPath+".Status").equals("Sent")) return;
			p.sendMessage(Message.COMMENT_NOTIFICATION.get().replace("_Player_", ConfigFile.REPORTS.get().getString(commentPath+".Author"))
					.replace("_Reported_", r.getPlayerName("Reported", false)).replace("_Time_", MessageUtils.convertToSentence(MessageUtils.getSeconds(MessageUtils.getNowDate())-MessageUtils.getSeconds(r.getDate())))
					.replace("_Message_", ConfigFile.REPORTS.get().getString(commentPath+".Message")));
			List<String> notifications = UserUtils.getNotifications(uuid.toString());
			notifications.remove(comment);
			UserUtils.setNotifications(uuid.toString(), notifications);
			ConfigFile.REPORTS.get().set(commentPath+".Status", "Read");
			ConfigFile.REPORTS.save();
		} catch(Exception invalidNotification) {
			;
		}
	}

	@SuppressWarnings("deprecation")
	public void comment(int reportNumber) {
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
			if(!(s instanceof Sign)) return;
			s.setLine(0, "§7[§6TigerReports§7]");
			s.setLine(1, "§e"+p.getName());
			s.setLine(2, "§8rédige un");
			s.setLine(3, "§8commentaire");
			s.update();
			
			commentingReport = reportNumber;
			save();
			Object tileEntity = ReflectionUtils.getDeclaredField(s,  "sign");
			ReflectionUtils.setDeclaredField(tileEntity, "isEditable", true);
			ReflectionUtils.setDeclaredField(tileEntity, "h", ReflectionUtils.getHandle(p));
			ReflectionUtils.sendPacket(p,  ReflectionUtils.getPacket("PacketPlayOutOpenSignEditor", ReflectionUtils.callDeclaredConstructor(ReflectionUtils.getNMSClass("BlockPosition"), s.getX(), s.getY(), s.getZ())));
		} catch(Exception Error) {
			;
		}
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
	
	public void setCommentingReport(int reportNumber) {
		commentingReport = reportNumber;
	}
	
	public int getCommentingReport() {
		return commentingReport;
	}
	
	public void setModifiedComment(int commentNumber) {
		modifiedComment = commentNumber;
	}
	
	public int getModifiedComment() {
		return modifiedComment;
	}
	
	public void save() {
		UserUtils.Users.put(uuid, this);
	}
	
}
