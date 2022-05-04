package fr.mrtigreroux.tigerreports.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

	/**
	 * 
	 * @param path
	 * @return null if file not found
	 */
	public static List<String> getFileLines(String path) {
		List<String> lines = new ArrayList<>();
		try (FileReader fr = new FileReader(path); BufferedReader br = new BufferedReader(fr);) {
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line);
			}
		} catch (FileNotFoundException noFile) {
			return null;
		} catch (IOException ex) {
			// Ignored
		}
		return lines;
	}

}
