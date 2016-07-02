package fr.mrtigreroux.tigerreports.commands;

import org.bukkit.command.Command;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * @author MrTigreroux
 */

public class TigerReportsCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
		s.sendMessage("§7§m---------------------------------------------------");
		s.sendMessage("§r                         §6TigerReports §7> §eAide");
		s.sendMessage("§7§m---------------------------------------------------");
		s.sendMessage("§7Commandes:");
		s.sendMessage("§7- §b/report <joueur> <raison> §7: §ePermet de signaler un joueur.");
		s.sendMessage("§7- §b/reports reload §7: §ePermet d'actualiser les fichiers de configuration.");
		s.sendMessage("§7- §b/reports §7: §ePermet d'afficher l'ensemble des signalements.");
		s.sendMessage("§7- §b/reports #<numéro du signalement> §7: §ePermet d'afficher un signalement.");
		s.sendMessage("§7- §b/reports user <joueur> §7: §ePermet de gérer les données d'un joueur.");
		s.sendMessage("§7- §b/reports stopcooldown <joueur> §7: §ePermet d'annuler l'attente d'un joueur.");
		s.sendMessage("§7- §b/reports notify §7: §ePermet d'activer ou désactiver les notifications de signalements.");
		s.sendMessage("§7- §b/reports archiveall §7: §ePermet d'archiver tous les signalements traités.");
		s.sendMessage("§7Le plugin §6TigerReports §7installé sur ce serveur a été réalisé par §a@MrTigreroux§7.");
		s.sendMessage("§7§m---------------------------------------------------");
		return true;
	}

}
