package fr.mrtigreroux.tigerreports.managers;

import fr.mrtigreroux.tigerreports.data.ConfigFile;

/**
 * @author MrTigreroux
 */

public class FilesManager {
	
	public static void loadFiles() {
		for(ConfigFile configFiles : ConfigFile.values()) configFiles.load();
	}
	
}
