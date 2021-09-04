package fr.mrtigreroux.tigerreports.data.constants;

import org.bukkit.Material;

import fr.mrtigreroux.tigerreports.objects.CustomItem;
import fr.mrtigreroux.tigerreports.utils.VersionUtils;

public class MenuRawItem {

	public static CustomItem GUI, GREEN_CLAY, RED_CLAY, YELLOW_CLAY, BLUE_CLAY, BLACK_CLAY, GOLDEN_AXE, WRITABLE_BOOK,
	        EMPTY_MAP;

	public static void init() {
		if (VersionUtils.isVersionLower1_13()) {
			GUI = createCustomItem("STAINED_GLASS_PANE", 7).name(" ");
			GREEN_CLAY = createCustomItem("STAINED_CLAY", 5);
			RED_CLAY = createCustomItem("STAINED_CLAY", 14);
			YELLOW_CLAY = createCustomItem("STAINED_CLAY", 4);
			BLUE_CLAY = createCustomItem("STAINED_CLAY", 3);
			BLACK_CLAY = createCustomItem("STAINED_CLAY", 9);
			GOLDEN_AXE = createCustomItem("GOLD_AXE", 0);
			WRITABLE_BOOK = createCustomItem("BOOK_AND_QUILL", 0);
			EMPTY_MAP = createCustomItem("EMPTY_MAP", 0);
		} else {
			GUI = createCustomItem(Material.GRAY_STAINED_GLASS_PANE).name(" ");
			GREEN_CLAY = createCustomItem(Material.LIME_TERRACOTTA);
			RED_CLAY = createCustomItem(Material.RED_TERRACOTTA);
			YELLOW_CLAY = createCustomItem(Material.YELLOW_TERRACOTTA);
			BLUE_CLAY = createCustomItem(Material.LIGHT_BLUE_TERRACOTTA);
			BLACK_CLAY = createCustomItem(Material.CYAN_TERRACOTTA);
			GOLDEN_AXE = createCustomItem(Material.GOLDEN_AXE);
			WRITABLE_BOOK = createCustomItem(Material.WRITABLE_BOOK);
			EMPTY_MAP = createCustomItem(Material.FILLED_MAP);
		}

	}

	private static CustomItem createCustomItem(String oldMaterialName, int damage) {
		return new CustomItem().type(Material.matchMaterial(oldMaterialName)).damage((short) damage).hideFlags(true);
	}

	private static CustomItem createCustomItem(Material material) {
		return new CustomItem().type(material).hideFlags(true);
	}

}
