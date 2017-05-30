package violet.jpa;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Embeddable;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

@Embeddable
public class Image {
	private static Path staticPath;
	private static URI staticURI;
	
	private String filename;
	
	private static void loadProperties() {
		try {
			Properties properties = new Properties();
			InputStream propertiesStream = Image.class.getClassLoader().getResourceAsStream("violet.properties");
			if(propertiesStream != null)
				properties.load(propertiesStream);
			else
				Logger.getLogger(Image.class.getName()).log(Level.SEVERE, "violet.properties not found");
			
			staticPath = Paths.get(properties.getProperty("static-path", "/var/www/static/"));
			staticURI = new URI(properties.getProperty("static-uri", "/static/"));
		} catch(IOException e) {
			Logger.getLogger(Image.class.getName()).log(Level.SEVERE, "Failed to load violet properties", e);
		} catch (URISyntaxException e) {
			Logger.getLogger(Image.class.getName()).log(Level.SEVERE, "Invalid static-uri in violet.properties", e);
		}
	}
	
	public Image() {
		if(staticPath == null || staticURI == null)
			loadProperties();
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public Path getPath() {
		return staticPath.resolve(filename);
	}
	
	public URI getURL() {
		return staticURI.resolve(filename);
	}
	
	public static Image saveImage(URL url) {
		Image out = new Image();
		
		URLConnection connection;
		try {
			connection = url.openConnection();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		String contentType = connection.getContentType();
		
		String extension;
		try {
			MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
			MimeType mimeType = allTypes.forName(contentType);
			extension = mimeType.getExtension();
		} catch(MimeTypeException e) {
			String urlFilename = url.getFile();
			extension = urlFilename.substring(urlFilename.indexOf("."));
		}
		
		out.setFilename(UUID.randomUUID().toString() + extension);
		
		FileOutputStream outputStream = null;
		try {
			File file = out.getPath().toFile();
			file.createNewFile();
			outputStream = new FileOutputStream(file);
			
			ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
			outputStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch(IOException e) {
			Logger.getLogger(Image.class.getName()).log(Level.SEVERE, "Failed to save image", e);
			return null;
		} finally {
			if(outputStream != null) {
				try {
					outputStream.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return out;
	}
}
