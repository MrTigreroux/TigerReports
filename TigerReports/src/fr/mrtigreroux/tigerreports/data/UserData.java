package fr.mrtigreroux.tigerreports.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.objects.menus.Menu;

/**
 * @author MrTigreroux
 */

public class UserData {

	public static HashMap<UUID, Menu>     MenuOpened = new HashMap<UUID, Menu>();

	public static HashMap<UUID, Integer>  ReportCommenting = new HashMap<UUID, Integer>();
	public static HashMap<UUID, Integer>  CommentModified = new HashMap<UUID, Integer>();
	public static HashMap<UUID, Material> SignMaterial = new HashMap<UUID, Material>();
	public static HashMap<UUID, Byte>     SignData = new HashMap<UUID, Byte>();
	public static HashMap<UUID, String>   LastMessages = new HashMap<UUID, String>();

	public static ArrayList<UUID>         NotificationsDisabled = new ArrayList<UUID>();
	
}
