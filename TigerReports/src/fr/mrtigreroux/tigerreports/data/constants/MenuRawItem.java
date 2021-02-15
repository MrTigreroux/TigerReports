package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;

public class MenuRawItem {

	public static CustomItem GUI, GREEN_CLAY, RED_CLAY, YELLOW_CLAY, BLUE_CLAY, BLACK_CLAY, GOLDEN_AXE, WRITABLE_BOOK, EMPTY_MAP;

	public static void init() {
		if (VersionUtils.isVersionLess1_13()) {
			GUI = getCustomItem("STAINED_GLASS_PANE", 7).name(" ");
			GREEN_CLAY = getCustomItem("STAINED_CLAY", 5);
			RED_CLAY = getCustomItem("STAINED_CLAY", 14);
			YELLOW_CLAY = getCustomItem("STAINED_CLAY", 4);
			BLUE_CLAY = getCustomItem("STAINED_CLAY", 3);
			BLACK_CLAY = getCustomItem("STAINED_CLAY", 9);
			GOLDEN_AXE = getCustomItem("GOLD_AXE", 0);
			WRITABLE_BOOK = getCustomItem("BOOK_AND_QUILL", 0);
			EMPTY_MAP = getCustomItem("EMPTY_MAP", 0);
		} else {
			GUI = getCustomItem(Material.GRAY_STAINED_GLASS_PANE).name(" ");
			GREEN_CLAY = getCustomItem(Material.LIME_TERRACOTTA);
			RED_CLAY = getCustomItem(Material.RED_TERRACOTTA);
			YELLOW_CLAY = getCustomItem(Material.YELLOW_TERRACOTTA);
			BLUE_CLAY = getCustomItem(Material.LIGHT_BLUE_TERRACOTTA);
			BLACK_CLAY = getCustomItem(Material.CYAN_TERRACOTTA);
			GOLDEN_AXE = getCustomItem(Material.GOLDEN_AXE);
			WRITABLE_BOOK = getCustomItem(Material.WRITABLE_BOOK);
			EMPTY_MAP = getCustomItem(Material.FILLED_MAP);
		}

	}

	private static CustomItem getCustomItem(String oldMaterialName, int damage) {
		return new CustomItem().type(Material.matchMaterial(oldMaterialName)).damage((short) damage).hideFlags(true);
	}

	private static CustomItem getCustomItem(Material material) {
		return new CustomItem().type(material).hideFlags(true);
	}

}
