package fr.mrtigreroux.tigerreports.utils;

import org.bukkit.Bukkit;

import fr.mrtigreroux.tigerreports.logs.Logger;

/**
 * @author MrTigreroux
 */
public class VersionUtils {
    
    private static final int MAX_VERSION_TOKEN_VALUE = 100;
    private static int minecraftVersion = 0;
    
    private VersionUtils() {}
    
    private static int getMinecraftVersion() {
        if (minecraftVersion == 0) {
            String bukkitVersion = Bukkit.getVersion();
            final String fver = bukkitVersion;
            Logger.MAIN.info(() -> "bukkit version: " + fver);
            try {
                minecraftVersion = toInt(
                        bukkitVersion.substring(bukkitVersion.indexOf('(') + 5, bukkitVersion.length() - 1)
                );
            } catch (NumberFormatException e) {
                Logger.CONFIG.error(
                        ConfigUtils.getInfoMessage(
                                "Failed to extract the Minecraft version (bukkit version = "
                                        + bukkitVersion + "):",
                                "La version Minecraft n'a pas pu etre extraite (bukkit version = "
                                        + bukkitVersion + "):"
                        ),
                        e
                );

                Logger.CONFIG.warn(() -> ConfigUtils.getInfoMessage(
                        "Attempting to fetch version using method 2 (NMS package identifier)...",
                        "Essai de récupération de la version en utilisant la méthode 2 (identifiant de package NMS)..."
                ));

                fetchVersionViaNMS();
            }
            Logger.MAIN.info(
                    () -> "MC version: " + minecraftVersion + ", checks: old: " + isOldVersion()
                            + ", <1.9: " + isVersionLower1_9() + ", <1.13: " + isVersionLower1_13()
                            + ", >=1.16: " + isVersionAtLeast1_16()
            );
        }
        return minecraftVersion;
    }

    public static void fetchVersionViaNMS() {
        String nmsPrefix = "org.bukkit.craftbukkit.v1_8_R3.CraftServer"
                .replace("org.bukkit.craftbukkit", "net.minecraft.server");
        String version;

        try {
            version = nmsPrefix.split("\\.")[3]; // "vX_X_RX"
        } catch (IndexOutOfBoundsException e) {
            Logger.CONFIG.error(ConfigUtils.getInfoMessage(
                    "Failed to extract the Minecraft version from NMS package (package string = "
                            + nmsPrefix + "):",
                    "La version Minecraft n'a pas pu etre extraite (package string = "
                            + nmsPrefix + "):"
            ), e);
            return;
        }

        System.out.println(version);

        int subVersionIndex = version.indexOf("_R");
        int mainVersionIndex = version.indexOf("v") + 1;

        String versionString = version.substring(mainVersionIndex, subVersionIndex).replace("_", ".");
        minecraftVersion = toInt(versionString);
    }
    
    public static boolean isOldVersion() {
        return getMinecraftVersion() < 10800;
    }
    
    public static boolean isVersionLower1_9() {
        return getMinecraftVersion() < 10900;
    }
    
    public static boolean isVersionLower1_13() {
        return getMinecraftVersion() < 11300;
    }
    
    public static boolean isVersionAtLeast1_16() {
        return getMinecraftVersion() >= 11600;
    }
    
    public static int toInt(String version) throws NumberFormatException {
        if (version == null) {
            return -1;
        }
        
        String[] verTokens = version.split("\\.");
        int result = 0;
        int decimal = verTokens.length >= 3 ? 1 : 100; // 1.2 = 1.2.0 = 1 20 00
        for (int i = verTokens.length - 1; i >= 0; i--) {
            result += Integer.parseInt(verTokens[i]) * decimal;
            decimal *= MAX_VERSION_TOKEN_VALUE;
        }
        return result;
    }
    
}
