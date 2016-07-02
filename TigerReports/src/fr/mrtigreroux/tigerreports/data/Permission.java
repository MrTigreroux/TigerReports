package fr.mrtigreroux.tigerreports.data;

/**
 * @author MrTigreroux
 */

public enum Permission {

	REPORT, STAFF, ARCHIVE, REMOVE, ADVANCED, MANAGE;
	
	public String get() {
		return "tigerreports."+toString().toLowerCase();
	}
	
}
