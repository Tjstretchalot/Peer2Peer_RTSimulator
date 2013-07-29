package me.timothy.dcrts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class ResourceManager {
	private static Map<String, Image> loadedImages = new HashMap<>();;
	
	public static void loadResource(String str) throws IOException, SlickException {
		File asFile = new File("res", str);
		
		if(!asFile.exists()) {
			InputStream inStream = ResourceManager.class.getResourceAsStream("../res/" + str);
			
			new File(asFile.getParent()).mkdirs();
			asFile.createNewFile();
			
			Files.copy(inStream, asFile.toPath());
			inStream.close();
			
			inStream = new FileInputStream(asFile);
		}
		
		loadedImages.put(str, new Image(asFile.getAbsolutePath()));
	}
	
	public static List<String> getAllResourceFiles() throws IOException {
		List<String> result = new ArrayList<>();
		
		File resourcesFile = new File("res", "resources.txt");
		
		if(!resourcesFile.exists()) {
			File parent = new File(resourcesFile.getParent());
			if(!parent.exists())
				parent.mkdirs();
			URL resourcesUrl = new URL("http://umad-barnyard.com/dcrts/resources/resources.txt");
			HttpURLConnection conn = (HttpURLConnection) resourcesUrl.openConnection();
			
			InputStream inStream = conn.getInputStream();
			Files.copy(inStream, resourcesFile.toPath());
			inStream.close();
			conn.disconnect();
		}
		
		BufferedReader inReader = new BufferedReader(new FileReader(resourcesFile));
		String ln;
		while((ln = inReader.readLine()) != null) {
			result.add(ln);
		}
		inReader.close();
		
		return result;
	}

	public static Image getResource(String string) {
		return loadedImages.get(string);
	}

}
