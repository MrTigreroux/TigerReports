package fr.mrtigreroux.tigerreports.data;

import org.bukkit.ChatColor;

import fr.mrtigreroux.tigerreports.managers.FilesManager;
import fr.mrtigreroux.tigerreports.utils.ConfigUtils;

/**
 * @author MrTigreroux
 */

public enum Message {

	RELOAD("Messages.Reload", "§7[§6TigerReports§7] §eLes fichiers de configuration ont été actualisés."),
	REPORT_SENT("Messages.Report-sent", "§7[§6Report§7] §eVous avez signalé le joueur §6_Player_ §epour: §7_Reason_§e."),
	ALERT("Messages.Alert", "§7[§cReport§7] §a_Signalman_ §7» §c_Reported_§7: §6_Reason_§7."),
	ALERT_DETAILS("Messages.Alert-details", "§6Clic gauche §7pour afficher les détails du §c_Report_§7."),
	TELEPORT_CURRENT_LOCATION("Messages.Teleport-to-current-location", "§7[§6Report§7] §eVous avez été téléporté au joueur _Player_§e."),
	TELEPORT_OLD_LOCATION("Messages.Teleport-to-old-location", "§7[§6Report§7] §eVous avez été téléporté au joueur _Player_ §elors du signalement."),
	STAFF_PUNISH("Messages.Staff-punish", "§7[§6Reports§7] §a_Player_ §evient de §csanctionner §eet traiter comme §cfaux §ele signalement de §6_Signalman_ §ependant _Time_§e."),
	PUNISHED("Messages.Punished", "§7[§6Reports§7] §eVous venez d'être §csanctionné §epour votre signalement jugé abusif. Vous ne pouvez plus signaler de joueur pendant §6_Time_§e."),
	STAFF_PROCESS("Messages.Staff-process", "§7[§6Reports§7] §a_Player_ §evient de §btraiter §ele _Report_ §eavec l'appréciation: _Appreciation_§e."),
	STAFF_ARCHIVEALL("Messages.Staff-archiveall", "§7[§6Reports§7] §a_Player_ §evient d'§8archiver §etous les signalements traités."),
	STAFF_STOPCOOLDOWN("Messages.Staff-stop-cooldown", "§7[§6Reports§7] §a_Player_ §evient de §cretirer §el'attente du joueur §6_Target_ §eavant son prochain signalement."),
	STAFF_ARCHIVE("Messages.Staff-archive", "§7[§6Reports§7] §a_Player_ §evient d'§8archiver §ele _Report_§e."),
	STAFF_REMOVE("Messages.Staff-remove", "§7[§6Reports§7] §a_Player_ §evient de §csupprimer §ele _Report_§e."),
	STAFF_MAX_REPORTS_REACHED("Messages.Staff-max-reports-reached", "§7[§6Reports§7] §c§lLe maximum de signalements configuré §7§l(§e§l_Number_§7§l) §c§la été atteint. Les nouveaux signalements ne sont plus sauvegardés."),
	COOLDOWN_STOPPED("Messages.Cooldown-stopped", "§7[§6Reports§7] §eLe temps d'attente avant votre prochain signalement vient d'être retiré, vous pouvez maintenant utiliser la commande §b/report§e."),
	NOTIFICATIONS("Messages.Notifications", "§7[§6Reports§7] §eLes notifications de signalements ont été _State_§e."),
	
	INVALID_SYNTAX("ErrorMessages.Invalid-syntax", "§7[§6Reports§7] §eVeuillez détailler votre commande tel que §b_Command_§e."),
	PERMISSION_COMMAND("ErrorMessages.Permission-command", "§7[§6Reports§7] §cVous n'avez pas accès à cette commande."),
	PERMISSION_ACCESS_DETAILS("ErrorMessages.Permission-access-details", "§7[§6Reports§7] §cVous ne pouvez pas accéder aux détails du _Report_§c."),
	PLAYER_ONLY("ErrorMessages.Player-only", "TigerReports > Cette commande est reservee aux joueurs."),
	COOLDOWN("ErrorMessages.Cooldown", "§7[§6Reports§7] §cVeuillez patienter encore §e_Time_ §cavant de signaler un joueur."),
	REPORT_ONESELF("ErrorMessages.Report-oneself", "§7[§6Reports§7] §cVous ne pouvez pas vous signaler vous-même."),
	REPORTED_OFFLINE("ErrorMessages.Reported-offline", "§7[§6Reports§7] §cVous ne pouvez pas signaler un joueur déconnecté §7(§e_Player_§7)§c."),
	TOO_SHORT_REASON("ErrorMessages.Too-short-reason", "§7[§6Reports§7] §cVous avez indiqué une raison trop courte §7(§e_Reason_§7)§c."),
	PLAYER_OFFLINE("ErrorMessages.Player-offline", "§7[§6Reports§7] §cLe joueur §e_Player_ §cn'est plus connecté."),
	LOCATION_UNKNOWN("ErrorMessages.Location-unknown", "§7[§6Reports§7] §cLa position du joueur §e_Player_ §clors du signalement est inconnue."),
	INVALID_REPORTNUMBER("ErrorMessages.Invalid-report-number", "§7[§6Reports§7] §cVous avez indiqué un numéro de signalement invalide §7(§e_Number_§7)§c."),
	INVALID_REPORT("ErrorMessages.Invalid-report", "§7[§6Reports§7] §cLe signalement _Report_ §cest invalide."),
	INVALID_PLAYER("ErrorMessages.Invalid-player", "§7[§6Reports§7] §cVous avez indiqué un joueur invalide §7(§e_Player_§7)§c."),
	
	SIGNALMAN("Menus.Signalman", "§7Signaleur: §a_Player_"),
	REPORTED("Menus.Reported", "§7Signalé: §c_Player_"),
	REPORT("Menus.Report", "_Report_"),
	REPORT_DETAILS("Menus.Report-details", "//§7Statut: _Status_//§7Date: §e_Date_// //§7Signaleur: §a_Signalman_//§7Signalé: §c_Reported_//§7Raison: §6_Reason_ §r_Actions_"),
	REPORT_SHOW_ACTION("Menus.Report-show-action", "// //§6Clic gauche §7pour afficher les détails."),
	REPORT_REMOVE_ACTION("Menus.Report-remove-action", "//§6Touche de jet §7pour supprimer."),
	REPORTS_TITLE("Menus.Reports-title", "§6Reports §7> §ePage _Page_"),
	REPORTS("Menus.Reports", "§eSignalements"),
	REPORTS_DETAILS("Menus.Reports-details", "//§6Clic §7pour afficher tous les signalements."),
	REPORT_TITLE("Menus.Report-title", "§6Report §7> §e_Report_"),
	TELEPORT_TO_CURRENT_POSITION("Menus.Teleport-to-current-position", "//§6Clic gauche §7pour vous téléporter à la//§7position actuelle du joueur §e_Player_§7."),
	CAN_NOT_TELEPORT_TO_CURRENT_POSITION("Menus.Can-not-teleport-to-current-position", "//§6§mClic gauche §7§mpour vous téléporter à la//§7§mposition actuelle du joueur §e§m_Player_§7§m."),
	TELEPORT_TO_OLD_POSITION("Menus.Teleport-to-old-position", "//§6Clic droit §7pour vous téléporter à l'ancienne//§7position du joueur §e_Player_§7."),
	CAN_NOT_TELEPORT_TO_OLD_POSITION("Menus.Can-not-teleport-to-old-position", "//§6§mClic droit §7§mpour vous téléporter à l'ancienne//§7§mposition du joueur §e§m_Player_§7§m."),
	PUNISH_ABUSE("Menus.Punish-abuse", "§eSignalement abusif"),
	PUNISH_ABUSE_DETAILS("Menus.Punish-abuse-details", "//§6Clic §7pour sanctionner le joueur §a_Player_//§7ayant envoyé ce signalement."),
	CHANGE_STATUS("Menus.Change-status", "§eMarquer comme: §r_Status_"),
	CHANGE_STATUS_DETAILS("Menus.Change-status-details", "//§6Clic §7pour définir le statut du signalement//§7comme: §r_Status_§7."),
	ARCHIVE("Menus.Archive", "§8Archiver le signalement"),
	ARCHIVE_DETAILS("Menus.Archive-details", "//§6Clic §7pour archiver le signalement."),
	DATA("Menus.Data", "§eDonnées collectées"),
	DATA_DETAILS("Menus.Data-details", "//§eSignalé: §c_Reported_//_DefaultData_//_AdvancedData_"),
	DEFAULT_DATA("Menus.Default-data", " §7Gamemode: _Gamemode_§7,  §7Au sol: _OnGround_// §7Sneak: _Sneak_§7,  §7Sprint: _Sprint_// §7Vie: §c_Health_§7,  §7Nourriture: §6_Food_// §7Effets: _Effects_"),
	PLAYER_WAS_OFFLINE("Menus.Player-was-offline", "§7Le joueur était déconnecté lors//§7du signalement."),
	EFFECT("Menus.Effect", "// §7- §b_Type_ _Amplifier_ §7(§8_Duration_ secondes§7)"),
	ADVANCED_DATA_REPORTED("Menus.Advanced-data-reported", " §7UUID: §8_UUID_// §7IP: §e_IP_//"),
	ADVANCED_DATA_SIGNALMAN("Menus.Advanced-data-signalman", "//§eSignaleur: §a_Player_// §7UUID: §8_UUID_// §7IP: §e_IP_"),
	COMMENTS("Menus.Comments", "§eCommentaires du signalement"),
	COMMENTS_DETAILS("Menus.Comments-details", "//§6Clic §7pour afficher les commentaires//§7du signalement."),
	REMOVE("Menus.Remove", "§cSupprimer le signalement"),
	REMOVE_DETAILS("Menus.Remove-details", "//§6Clic §7pour supprimer définitivement//§7le signalement."),
	CONFIRM_REMOVE_TITLE("Menus.Confirm-remove-title", "§6Supprimer §7> _Report_"),
	CONFIRM_REMOVE("Menus.Confirm-remove", "§a§lConfirmer la suppression"),
	CONFIRM_REMOVE_DETAILS("Menus.Confirm-remove-details", "//§6Clic §7pour supprimer définitivement //§7le §c_Report_§7."),
	CANCEL_REMOVE("Menus.Cancel-remove", "§c§lAnnuler la suppression"),
	CANCEL_REMOVE_DETAILS("Menus.Cancel-remove-details", "//§6Clic §7pour annuler la suppression."),
	CONFIRM_ARCHIVE_TITLE("Menus.Confirm-archive-title", "§6Archiver §7> _Report_"),
	CONFIRM_ARCHIVE("Menus.Confirm-archive", "§a§lConfirmer l'archivage"),
	CONFIRM_ARCHIVE_DETAILS("Menus.Confirm-archive-details", "//§6Clic §7pour archiver le §c_Report_§7."),
	CANCEL_ARCHIVE("Menus.Cancel-archive", "§c§lAnnuler l'archivage"),
	CANCEL_ARCHIVE_DETAILS("Menus.Cancel-archive-details", "//§6Clic §7pour annuler l'archivage."),
	PROCESS_TITLE("Menus.Process-title", "§6Traiter §7> _Report_"),
	PROCESS("Menus.Process", "§eTraiter comme: _Appreciation_"),
	PROCESS_DETAILS("Menus.Process-details", "//§6Clic §7pour traiter le signalement //§7comme: _Appreciation_§7."),
	CANCEL_PROCESS("Menus.Cancel-process", "§cAnnuler le traitement"),
	CANCEL_PROCESS_DETAILS("Menus.Cancel-process-details", "//§6Clic §7pour annuler le traitement."),
	COMMENTS_TITLE("Menus.Comments-title", "§6Messages §7> _Report_"),
	COMMENT("Menus.Comment", "§eCommentaire §7#_Number_"),
	COMMENT_DETAILS("Menus.Comment-details", "//§7Auteur: _Author_//§7Date: §e_Date_//§7Message: §f_Message_ _Action_"),
	COMMENT_REMOVE_ACTION("Menus.Comment-remove-action", "// //§6Touche de jet §7pour supprimer."),
	WRITE_COMMENT("Menus.Write-comment", "§eRédiger un commentaire"),
	WRITE_COMMENT_DETAILS("Menus.Write-comment-details", "//§6Clic §7pour rédiger un nouveau//§7commentaire sur ce signalement."),
	USER_TITLE("Menus.User-title", "§6Joueur §7> §e_Target_"),
	USER("Menus.User", "§7Joueur: §e_Target_"),
	USER_APPRECIATION("Menus.User-appreciation", "§7Appréciation: _Appreciation_ §7x§b_Number_"),
	USER_APPRECIATION_DETAILS("Menus.User-appreciation-details", "//§6Clic gauche §7pour ajouter une appréciation.//§6Clic droit §7pour retirer une appréciation."),
	COOLDOWN_STATUS("Menus.Cooldown-status", "§7Attente: §e_Time_"),
	COOLDOWN_STATUS_DETAILS("Menus.Cooldown-status-details", "//§6Clic §7pour annuler l'attente du//§7joueur §e_Player_§7."),
	PAGE_SWITCH_PREVIOUS("Menus.Switch-to-previous-page", "§6§l< §7Page précédente"),
	PAGE_SWITCH_NEXT("Menus.Switch-to-next-page", "§7Page suivante §6§l>"),
	CLOSE("Menus.Close", "§cFermer"),
	
	REPORT_NAME("Words.Report-name", "§cReport §7#_Number_"),
	REPORTED_NAME("Words.Reported-name", "§c_Player_"),
	SIGNALMAN_NAME("Words.Signalman-name", "§a_Player_"),
	ONLINE_SUFFIX("Words.Online-suffix", " §7(§aconnecté§7)"),
	OFFLINE_SUFFIX("Words.Offline-suffix", " §7(§cdéconnecté§7)"),
	NOT_FOUND_MALE("Words.Not-found-male", "§cNon trouvé"),
	NOT_FOUND_FEMALE("Words.Not-found-female", "§cNon trouvée"),
	ACTIVATED("Words.Activated", "§aactivées"),
	DISABLED("Words.Disabled", "§cdésactivées"),
	NONE_MALE("Words.None-male", "§cAucun"),
	NONE_FEMALE("Words.None-female", "§cAucune"),
	SECOND("Words.Second", "seconde"),
	SECONDS("Words.Seconds", "secondes"),
	MINUTE("Words.Minute", "minute"),
	MINUTES("Words.Minutes", "minutes"),
	HOUR("Words.Hour", "heure"),
	HOURS("Words.Hours", "heures"),
	DAY("Words.Day", "jour"),
	DAYS("Words.Days", "jours"),
	WEEK("Words.Week", "semaine"),
	WEEKS("Words.Weeks", "semaines"),
	MONTH("Words.Month", "mois"),
	MONTHS("Words.Months", "mois"),
	YEAR("Words.Year", "année"),
	YEARS("Words.Years", "années"),
	WAITING("Words.Waiting", "§aEn attente"),
	IN_PROGRESS("Words.In-progress", "§6En cours"),
	IMPORTANT("Words.Important", "§cImportant"),
	DONE("Words.Done", "§bTraité"),
	APPRECIATION_SUFFIX("Words.Appreciation-suffix", " §7(_Appreciation_§7)"),
	TRUE("Words.True-appreciation", "§aVrai"),
	UNCERTAIN("Words.Uncertain-appreciation", "§6Incertain"),
	FALSE("Words.False-appreciation", "§cFaux"),
	YES("Words.Yes", "§aoui"),
	NO("Words.No", "§cnon"),
	SURVIVAL("Words.Survival", "§6survie"),
	CREATIVE("Words.Creative", "§bcréatif"),
	ADVENTURE("Words.Adventure", "§5aventure"),
	SPECTATOR("Words.Spectator", "§7spectateur");
	
	private final String path;
	private final String defaultMessage;
	
	Message(String path, String defaultMessage) {
		this.path = path;
		this.defaultMessage = defaultMessage;
	}
	
	public String get() {
		if(FilesManager.getMessages.contains(path) && FilesManager.getMessages.get(path) != null) return ChatColor.translateAlternateColorCodes(ConfigUtils.getColorCharacter(), FilesManager.getMessages.getString(path));
		else return defaultMessage;
	}
	
}
