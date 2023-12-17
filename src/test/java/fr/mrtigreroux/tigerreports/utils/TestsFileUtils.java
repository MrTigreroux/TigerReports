package fr.mrtigreroux.tigerreports.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author MrTigreroux
 */
public class TestsFileUtils {

    public static File getProjectFile(Path pathInProject) {
    	Path osPath = getProjectElementOSPath(pathInProject);
    	if (!Files.isRegularFile(osPath)) {
    		throw new IllegalArgumentException("The file " + pathInProject + " was not found.");
    	}
    	return Objects.requireNonNull(osPath.toFile());
    }

    public static Path getProjectDirectoryPath(Path pathInProject) {
    	Path osPath = getProjectElementOSPath(pathInProject);
    	if (!Files.isDirectory(osPath)) {
			throw new IllegalArgumentException("The directory " + pathInProject + " was not found.");
		}
    	return osPath;
    }

    public static Path getProjectElementOSPath(Path pathInProject) {
        return Paths.get(System.getProperty("user.dir")).resolve(pathInProject);
    }

}
