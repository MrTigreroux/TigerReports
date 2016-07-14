package fr.mrtigreroux.tigerreports.objects;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import fr.mrtigreroux.tigerreports.data.Message;
import fr.mrtigreroux.tigerreports.data.Permission;
import fr.mrtigreroux.tigerreports.data.UserData;
import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.objects.menus.ProcessMenu;
import fr.mrtigreroux.tigerreports.objects.menus.CommentsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ConfirmationMenu;
import fr.mrtigreroux.tigerreports.objects.menus.Menu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportMenu;
import fr.mrtigreroux.tigerreports.objects.menus.ReportsMenu;
import fr.mrtigreroux.tigerreports.objects.menus.UserMenu;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;
import fr.mrtigreroux.tigerreports.utils.MessageUtils;
import fr.mrtigreroux.tigerreports.utils.ReflectionUtils;
import fr.mrtigreroux.tigerreports.utils.ReportUtils;
import fr.mrtigreroux.tigerreports.utils.UserUtils;

/**
 * @author MrTigreroux
 */

public class User {
	
	private Player p;
	private UUID uuid;
	
	public User(Player p) {
		this.p = p;
		this.uuid = p.getUniqueId();
	}
	
	public Player getPlayer() {
		return p;
	}
	
	public String getCooldown() {
		String cooldown = FilesManager.getData.get("Data."+uuid+".Cooldown") != null ? FilesManager.getData.getString("Data."+uuid+".Cooldown") : MessageUtils.getNowDate();
		double seconds = MessageUtils.getSeconds(cooldown)-MessageUtils.getSeconds(MessageUtils.getNowDate());
		if(seconds <= 0) return null;
		return MessageUtils.convertToSentence(seconds);
	}
	
	public void startCooldown(double seconds) {
		FilesManager.getData.set("Data."+uuid+".Cooldown", MessageUtils.convertToDate(MessageUtils.getSeconds(MessageUtils.getNowDate())+seconds));
		FilesManager.saveData();
	}

	public void stopCooldown(String author) {
		MessageUtils.sendStaffMessage(Message.STAFF_STOPCOOLDOWN.get().replaceAll("_Player_", author).replaceAll("_Target_", p.getName()), ConfigUtils.getStaffSound());
		p.sendMessage(Message.COOLDOWN_STOPPED.get());
		FilesManager.getData.set("Data."+uuid+".Cooldown", null);
		FilesManager.saveData();
	}

	public void openReportsMenu(int page, boolean sound) {
		new ReportsMenu(this, page).open(sound);
	}
	
	public void openReportMenu(int reportNumber) {
		new ReportMenu(this, reportNumber).open(true);
	}

	public void openAppreciationMenu(int reportNumber) {
		new ProcessMenu(this, reportNumber).open(true);
	}
	
	public void openConfirmationMenu(int reportNumber, String action) {
		new ConfirmationMenu(this, reportNumber, action).open(true);
	}

	public void openCommentsMenu(int page, int reportNumber) {
		new CommentsMenu(this, page, reportNumber).open(true);
	}

	public void openUserMenu(String target) {
		new UserMenu(this, target).open(true);
	}
	
	public void setOpenedMenu(Menu menu) {
		UserData.MenuOpened.put(uuid, menu);
	}

	public Menu getOpenedMenu() {
		return UserData.MenuOpened.containsKey(uuid) ? UserData.MenuOpened.get(uuid) : null;
	}
	
	public boolean hasPermission(Permission permission) {
		return p.hasPermission(permission.get());
	}

	@SuppressWarnings("deprecation")
	public void comment(int reportNumber) {
		try {
			Location loc = p.getLocation();
			World world = loc.getWorld();
			Block b = world.getBlockAt(loc.getBlockX(), world.getMaxHeight()-1, loc.getBlockZ());
			UserData.SignMaterial.put(uuid, b.getType());
			UserData.SignData.put(uuid, b.getData());
			
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
			
			UserData.ReportCommenting.put(uuid, reportNumber);
			Object tileEntity = ReflectionUtils.getDeclaredField(s,  "sign");
			ReflectionUtils.setDeclaredField(tileEntity, "isEditable", true);
			ReflectionUtils.setDeclaredField(tileEntity, "h", ReflectionUtils.getHandle(p));
			ReflectionUtils.sendPacket(p,  ReflectionUtils.getPacket("PacketPlayOutOpenSignEditor", ReflectionUtils.callDeclaredConstructor(ReflectionUtils.getNMSClass("BlockPosition"), s.getX(), s.getY(), s.getZ())));
		} catch(Exception Error) {}
	}
	
	@SuppressWarnings("deprecation")
	public void updateSignBlock(Block b) {
		b.setType(UserData.SignMaterial.containsKey(uuid) ? UserData.SignMaterial.get(uuid) : Material.AIR);
		Block support = b.getRelative(BlockFace.DOWN);
		if(support.getType() == Material.BEDROCK) support.setType(Material.AIR);
		if(UserData.SignData.containsKey(uuid)) b.setData(UserData.SignData.get(uuid));
		UserData.SignMaterial.remove(uuid);
		UserData.SignData.remove(uuid);
	}
	
	public void sendNotification(String comment) {
		try {
			int reportNumber = Integer.parseInt(comment.split(":")[0].replaceFirst("Report#", ""));
			String commentPath = ReportUtils.getConfigPath(reportNumber)+".Comments.Comment"+comment.split(":")[1].replaceFirst("Comment#", "");
			if(!FilesManager.getReports.getString(commentPath+".Status").equals("Sent")) return;
			p.sendMessage(Message.COMMENT_NOTIFICATION.get().replaceAll("_Player_", FilesManager.getReports.getString(commentPath+".Author"))
					.replaceAll("_Reported_", ReportUtils.getPlayerName("Reported", reportNumber, false)).replaceAll("_Time_", MessageUtils.convertToSentence(MessageUtils.getSeconds(MessageUtils.getNowDate())-MessageUtils.getSeconds(ReportUtils.getDate(reportNumber))))
					.replaceAll("_Message_", FilesManager.getReports.getString(commentPath+".Message")));
			List<String> notifications = UserUtils.getNotifications(uuid.toString());
			notifications.remove(comment);
			UserUtils.setNotifications(uuid.toString(), notifications);
			FilesManager.getReports.set(commentPath+".Status", "Read");
			FilesManager.saveReports();
		} catch(Exception invalidNotification) {
			;
		}
	}
	
}
