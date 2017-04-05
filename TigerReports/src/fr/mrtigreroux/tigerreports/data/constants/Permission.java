package fr.mrtigreroux.tigerreports.data.constants;

/**
 * @author MrTigreroux
 */

public enum Permission {

	REPORT, STAFF, TELEPORT, EXEMPT, ARCHIVE, REMOVE, ADVANCED, MANAGE;
	
	public String get() {
		return "tigerreports."+toString().toLowerCase();
	}
	
}
