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
			s.sendMessage("§7- §b/report <player> <reason> §7: §eAllows to report a player.");
			s.sendMessage("§7- §b/reports reload §7: §eAllows to reload the plugin.");
			s.sendMessage("§7- §b/reports §7: §eAllows to show all reports.");
			s.sendMessage("§7- §b/reports #<report id> §7: §eAllows to show specific report.");
			s.sendMessage("§7- §b/reports user <player> §7: §eAllows to manage data of an user.");
			s.sendMessage("§7- §b/reports stopcooldown <player> §7: §eAllows to remove cooldown of a player.");
			s.sendMessage("§7- §b/reports notify §7: §eAllows to enable or disable reports notifications.");
			s.sendMessage("§7- §b/reports archiveall §7: §eAllows to archive all done reports.");
			s.sendMessage("§7- §b/reports archives §7: §eAllows to see all archived reports.");
			s.sendMessage("§7Plugin §6TigerReports §7installed on this server has been created by §a@MrTigreroux§7.");
			s.sendMessage("§7§m---------------------------------------------------");
		} else {
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§r                         §6TigerReports §7> §eAide");
			s.sendMessage("§7§m---------------------------------------------------");
			s.sendMessage("§7Commandes:");
			s.sendMessage("§7- §b/report <joueur> <raison> §7: §ePermet de signaler un joueur.");
			s.sendMessage("§7- §b/reports reload §7: §ePermet de recharger le plugin.");
			s.sendMessage("§7- §b/reports §7: §ePermet d'afficher l'ensemble des signalements.");
			s.sendMessage("§7- §b/reports #<identifiant du signalement> §7: §ePermet d'afficher un signalement.");
			s.sendMessage("§7- §b/reports user <joueur> §7: §ePermet de gérer les données d'un joueur.");
			s.sendMessage("§7- §b/reports stopcooldown <joueur> §7: §ePermet d'annuler l'attente d'un joueur.");
			s.sendMessage("§7- §b/reports notify §7: §ePermet d'activer ou désactiver les notifications de signalements.");
			s.sendMessage("§7- §b/reports archiveall §7: §ePermet d'archiver tous les signalements traités.");
			s.sendMessage("§7- §b/reports archives §7: §ePermet d'afficher tous les signalements archivés.");
			s.sendMessage("§7Le plugin §6TigerReports §7installé sur ce serveur a été réalisé par §a@MrTigreroux§7.");
			s.sendMessage("§7§m---------------------------------------------------");
		}
	}

}
