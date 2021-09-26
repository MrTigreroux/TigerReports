package fr.mrtigreroux.tigerreports.commands;

import org.bukkit.command.CommandSender;

import fr.mrtigreroux.tigerreports.data.config.Message;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public class HelpCommand {

	public static void onCommand(CommandSender s) {
		String straightLine = "\u00A77\u00A7m---------------------------------------------------";
		String commands = Message.get("Messages.Help-commands");
		String[] commandLines = new String[0];
		if (commands != null && !commands.isEmpty())
			commandLines = commands.split(ConfigUtils.getLineBreakSymbol());

		if (ConfigUtils.getInfoLanguage().equalsIgnoreCase("French")) {
			s.sendMessage(straightLine);
			s.sendMessage("\u00A7r                         \u00A76TigerReports \u00A77> \u00A7eAide");
			s.sendMessage(straightLine);
			if (commandLines != null && commandLines.length > 0) {
				for (String line : commandLines) {
					s.sendMessage(line);
				}
			} else {
				s.sendMessage("\u00A77Commandes:");
				s.sendMessage("\u00A77- \u00A7b/report <joueur> (raison) \u00A77: \u00A7eSignale un joueur.");
				s.sendMessage("\u00A77- \u00A7b/reports reload \u00A77: \u00A7eRecharge le plugin.");
				s.sendMessage("\u00A77- \u00A7b/reports \u00A77: \u00A7eAffiche l'ensemble des signalements.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports <identifiant du signalement> \u00A77: \u00A7eAffiche un signalement.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports user <joueur> \u00A77: \u00A7eG\u00E8re les donn\u00E9es d'un joueur.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports stopcooldown <joueur> \u00A77: \u00A7eAnnule l'attente d'un joueur avant son prochain signalement.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports punish <joueur> <seconds> \u00A77: \u00A7eEmp\u00EAche les signalements d'un joueur pendant une certaine dur\u00E9e.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports notify \u00A77: \u00A7eActive ou d\u00E9sactive les notifications de signalements.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports <archive / delete> <identifiant du signalement> \u00A77: \u00A7eArchive/supprime le signalement.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports comment <identifiant du signalement> <commentaire>\u00A77: \u00A7eAjoute le commentaire au signalement.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports archiveall \u00A77: \u00A7eArchive tous les signalements trait\u00E9s.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports archives \u00A77: \u00A7eAffiche tous les signalements archiv\u00E9s.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports deleteall \u00A77: \u00A7eSupprime tous les signalements archiv\u00E9s.");
			}
			s.sendMessage(
			        "\u00A77Le plugin \u00A76TigerReports \u00A77install\u00E9 sur ce serveur a \u00E9t\u00E9 r\u00E9alis\u00E9 par \u00A7a@MrTigreroux\u00A77.");
			s.sendMessage(straightLine);
		} else {
			s.sendMessage(straightLine);
			s.sendMessage("\u00A7r                         \u00A76TigerReports \u00A77> \u00A7eHelp");
			s.sendMessage(straightLine);
			if (commandLines != null && commandLines.length > 0) {
				for (String line : commandLines) {
					s.sendMessage(line);
				}
			} else {
				s.sendMessage("\u00A77Commands:");
				s.sendMessage("\u00A77- \u00A7b/report <player> (reason) \u00A77: \u00A7eReports a player.");
				s.sendMessage("\u00A77- \u00A7b/reports reload \u00A77: \u00A7eReloads the plugin.");
				s.sendMessage("\u00A77- \u00A7b/reports \u00A77: \u00A7eDisplays all reports.");
				s.sendMessage("\u00A77- \u00A7b/reports <report id> \u00A77: \u00A7eDisplays a specific report.");
				s.sendMessage("\u00A77- \u00A7b/reports user <player> \u00A77: \u00A7eManages data of an user.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports stopcooldown <player> \u00A77: \u00A7eRemoves cooldown from a player before his next report.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports punish <player> <seconds> \u00A77: \u00A7ePrevents the player from reporting for a certain time.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports notify \u00A77: \u00A7eEnables or disables reports notifications.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports <archive / delete> <report id> \u00A77: \u00A7eArchives/deletes the report.");
				s.sendMessage(
				        "\u00A77- \u00A7b/reports comment <report id> <comment> \u00A77: \u00A7eAdds the comment to the report.");
				s.sendMessage("\u00A77- \u00A7b/reports archiveall \u00A77: \u00A7eArchives all done reports.");
				s.sendMessage("\u00A77- \u00A7b/reports archives \u00A77: \u00A7eDisplays all archived reports.");
				s.sendMessage("\u00A77- \u00A7b/reports deleteall \u00A77: \u00A7eDeletes all archived reports.");
			}
			s.sendMessage(
			        "\u00A77Plugin \u00A76TigerReports \u00A77installed on this server has been created by \u00A7a@MrTigreroux\u00A77.");
			s.sendMessage(straightLine);
		}
	}

}
