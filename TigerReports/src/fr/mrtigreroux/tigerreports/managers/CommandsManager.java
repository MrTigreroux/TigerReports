package fr.mrtigreroux.tigerreports.managers;

import fr.mrtigreroux.tigerreports.TigerReports;
import fr.mrtigreroux.tigerreports.commands.ReportCommand;
import fr.mrtigreroux.tigerreports.commands.ReportsCommand;

/**
 * @author MrTigreroux
 */

public class CommandsManager {
	
	public static TigerReports main = TigerReports.getInstance();
	
	public static void registerCommands() {
		main.getCommand("report").setExecutor(new ReportCommand());
		main.getCommand("reports").setExecutor(new ReportsCommand());
	}
	
}
