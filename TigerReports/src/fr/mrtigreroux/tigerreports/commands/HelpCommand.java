package fr.mrtigreroux.tigerreports.commands;

import org.bukkit.command.CommandSender;

import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class HelpCommand {
	
	public static void onCommand(CommandSender s) {
		if(ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§r                         §6TigerReports §7> §eHelp");
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§7Commands:");
			s.sendMessage("§7- §b/report <player> <reason> §7: §eReports a player.");
			s.sendMessage("§7- §b/reports reload §7: §eReloads the plugin.");
			s.sendMessage("§7- §b/reports §7: §eDisplays all reports.");
			s.sendMessage("§7- §b/reports #<report id> §7: §eDisplays specific report.");
			s.sendMessage("§7- §b/reports user <player> §7: §eManages data of an user.");
			s.sendMessage("§7- §b/reports stopcooldown <player> §7: §eRemoves cooldown of a player.");
			s.sendMessage("§7- §b/reports notify §7: §eEnables or disables reports notifications.");
			s.sendMessage("§7- §b/reports archiveall §7: §eArchives all done reports.");
			s.sendMessage("§7- §b/reports archives §7: §eDisplays all archived reports.");
			s.sendMessage("§7- §b/reports removeall §7: §eRemoves all archived reports.");
			s.sendMessage("§7Plugin §6TigerReports §7installed on this server has been created by §a@MrTigreroux§7.");
			s.sendMessage("§7§m---------------------------------------------------");
		} else {
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§r                         §6TigerReports §7> §eAide");
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§7Commandes:");
			s.sendMessage("§7- §b/report <joueur> <raison> §7: §eSignale un joueur.");
			s.sendMessage("§7- §b/reports reload §7: §eRecharge le plugin.");
			s.sendMessage("§7- §b/reports §7: §eAffiche l'ensemble des signalements.");
			s.sendMessage("§7- §b/reports #<identifiant du signalement> §7: §eAffiche un signalement.");
			s.sendMessage("§7- §b/reports user <joueur> §7: §eGère les données d'un joueur.");
			s.sendMessage("§7- §b/reports stopcooldown <joueur> §7: §eAnnule l'attente d'un joueur.");
			s.sendMessage("§7- §b/reports notify §7: §eActive ou désactive les notifications de signalements.");
			s.sendMessage("§7- §b/reports archiveall §7: §eArchive tous les signalements traités.");
			s.sendMessage("§7- §b/reports archives §7: §eAffiche tous les signalements archivés.");
			s.sendMessage("§7- §b/reports removeall §7: §eSupprime tous les signalements archivés.");
			s.sendMessage("§7Le plugin §6TigerReports §7installé sur ce serveur a été réalisé par §a@MrTigreroux§7.");
			s.sendMessage("§7§m---------------------------------------------------");
		}
	}

}
