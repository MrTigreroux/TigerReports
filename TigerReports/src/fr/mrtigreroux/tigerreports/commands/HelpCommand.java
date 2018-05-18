package fr.mrtigreroux.tigerreports.commands;

import org.bukkit.command.CommandSender;

import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class HelpCommand {
	
	public static void onCommand(CommandSender s) {
		if(ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
			s.sendMessage("\u00A7r                         \u00A76TigerReports \u00A77> \u00A7eHelp");
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
			s.sendMessage("\u00A77Commands:");
			s.sendMessage("\u00A77- \u00A7b/report <player> <reason> \u00A77: \u00A7eReports a player.");
			s.sendMessage("\u00A77- \u00A7b/reports reload \u00A77: \u00A7eReloads the plugin.");
			s.sendMessage("\u00A77- \u00A7b/reports \u00A77: \u00A7eDisplays all reports.");
			s.sendMessage("\u00A77- \u00A7b/reports #<report id> \u00A77: \u00A7eDisplays specific report.");
			s.sendMessage("\u00A77- \u00A7b/reports user <player> \u00A77: \u00A7eManages data of an user.");
			s.sendMessage("\u00A77- \u00A7b/reports stopcooldown <player> \u00A77: \u00A7eRemoves cooldown of a player.");
			s.sendMessage("\u00A77- \u00A7b/reports notify \u00A77: \u00A7eEnables or disables reports notifications.");
			s.sendMessage("\u00A77- \u00A7b/reports archiveall \u00A77: \u00A7eArchives all done reports.");
			s.sendMessage("\u00A77- \u00A7b/reports archives \u00A77: \u00A7eDisplays all archived reports.");
			s.sendMessage("\u00A77- \u00A7b/reports deleteall \u00A77: \u00A7eDeletes all archived reports.");
			s.sendMessage("\u00A77Plugin \u00A76TigerReports \u00A77installed on this server has been created by \u00A7a@MrTigreroux\u00A77.");
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
		} else {
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
			s.sendMessage("\u00A7r                         \u00A76TigerReports \u00A77> \u00A7eAide");
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
			s.sendMessage("\u00A77Commandes:");
			s.sendMessage("\u00A77- \u00A7b/report <joueur> <raison> \u00A77: \u00A7eSignale un joueur.");
			s.sendMessage("\u00A77- \u00A7b/reports reload \u00A77: \u00A7eRecharge le plugin.");
			s.sendMessage("\u00A77- \u00A7b/reports \u00A77: \u00A7eAffiche l'ensemble des signalements.");
			s.sendMessage("\u00A77- \u00A7b/reports #<identifiant du signalement> \u00A77: \u00A7eAffiche un signalement.");
			s.sendMessage("\u00A77- \u00A7b/reports user <joueur> \u00A77: \u00A7eG\u00E8re les donn\u00E9es d'un joueur.");
			s.sendMessage("\u00A77- \u00A7b/reports stopcooldown <joueur> \u00A77: \u00A7eAnnule l'attente d'un joueur.");
			s.sendMessage("\u00A77- \u00A7b/reports notify \u00A77: \u00A7eActive ou d\u00E9sactive les notifications de signalements.");
			s.sendMessage("\u00A77- \u00A7b/reports archiveall \u00A77: \u00A7eArchive tous les signalements trait\u00E9s.");
			s.sendMessage("\u00A77- \u00A7b/reports archives \u00A77: \u00A7eAffiche tous les signalements archiv\u00E9s.");
			s.sendMessage("\u00A77- \u00A7b/reports deleteall \u00A77: \u00A7eSupprime tous les signalements archiv\u00E9s.");
			s.sendMessage("\u00A77Le plugin \u00A76TigerReports \u00A77install\u00E9 sur ce serveur a \u00E9t\u00E9 r\u00E9alis\u00E9 par \u00A7a@MrTigreroux\u00A77.");
			s.sendMessage("\u00A77\u00A7m---------------------------------------------------");
		}
	}

}
