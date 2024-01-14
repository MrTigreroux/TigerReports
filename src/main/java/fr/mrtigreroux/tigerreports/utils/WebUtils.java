package fr.mrtigreroux.tigerreports.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import fr.mrtigreroux.tigerreports.tasks.ResultCallback;
import fr.mrtigreroux.tigerreports.tasks.TaskScheduler;

/**
 * @author MrTigreroux
 */
public class WebUtils {
    
    private WebUtils() {}
    
    public static void checkNewVersion(Plugin plugin, TaskScheduler taskScheduler,
            String resourceId, ResultCallback<String> resultCallback) {
        taskScheduler.runTaskAsynchronously(new Runnable() {
            
            @Override
            public void run() {
                String queryResult = sendQuery(
                        "https://api.spigotmc.org/legacy/update.php?resource=" + resourceId,
                        null
                );
                
                taskScheduler.runTask(new Runnable() {
                    
                    @Override
                    public void run() {
                        String newVersion = queryResult;
                        if (plugin.getDescription().getVersion().equals(newVersion)) {
                            newVersion = null;
                        }
                        
                        if (newVersion != null) {
                            Logger logger = Bukkit.getLogger();
                            logger.warning(MessageUtils.LINE);
                            String pluginName = plugin.getDescription().getName();
                            if (ConfigUtils.getInfoLanguage().equalsIgnoreCase("English")) {
                                logger.warning("[" + pluginName + "] The plugin has been updated.");
                                logger.warning(
                                        "The new version " + newVersion + " is available on:"
                                );
                            } else {
                                logger.warning("[" + pluginName + "] Le plugin a ete mis a jour.");
                                logger.warning(
                                        "La nouvelle version " + newVersion + " est disponible ici:"
                                );
                            }
                            logger.warning(
                                    "https://www.spigotmc.org/resources/" + pluginName.toLowerCase()
                                            + "." + resourceId + "/"
                            );
                            logger.warning(MessageUtils.LINE);
                        }
                        
                        resultCallback.onResultReceived(newVersion);
                    }
                    
                });
            }
            
        });
        
    }
    
    private static String sendQuery(String url, String data) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            if (data != null) {
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(data.getBytes("UTF-8"));
                } catch (IOException ignored) {}
            }
            
            try (
                    InputStream in = connection.getInputStream();
                    InputStreamReader inReader = new InputStreamReader(in);
                    BufferedReader bufReader = new BufferedReader(inReader)
            ) {
                return bufReader.readLine();
            } catch (IOException ignored) {
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
    
}
