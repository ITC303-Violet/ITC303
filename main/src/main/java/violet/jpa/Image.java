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
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Embeddable;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

/**
 * Stores information about images used throughout the system and their filename
 * Settings can be found in violet.properties
 * 
 * Implements Collection so #{empty image} can be used in xhtml (doesn't work without)
 * @author somer
 */
@Embeddable
@SuppressWarnings("rawtypes")
public class Image implements Collection {
	private static Path staticPath; // the path to save files to
	private static URI staticURI; // the url to prepend to filenames (e.g. /static/)
	
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
	
	/**
	 * Downloads and saves an image
	 * @param url
	 * @return Image object representing the downloaded image
	 */
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
		
		String extension; // find the extension of the image (e.g. png, jpg, etc)
		try { // first based on the provided mimetype
			MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
			MimeType mimeType = allTypes.forName(contentType);
			extension = mimeType.getExtension();
		} catch(MimeTypeException e) { // failing that, try using the filename
			String urlFilename = url.getFile();
			extension = urlFilename.substring(urlFilename.indexOf("."));
		}
		
		out.setFilename(UUID.randomUUID().toString() + extension); // generate a random filename with the prior worked out extension
		
		FileOutputStream outputStream = null;
		try { // download the write the image to our file
			File file = out.getPath().toFile();
			file.createNewFile();
			outputStream = new FileOutputStream(file);
			
			ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
			outputStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		} catch(IOException e) {
			Logger.getLogger(Image.class.getName()).log(Level.SEVERE, "Failed to save image");
			return null;
		} finally {
			if(outputStream != null) {
				try { // always close the stream regardless of what happens
					outputStream.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return out;
	}
	
	public boolean isEmpty() { // used in xhtml
		return filename == null || filename.isEmpty();
	}

	// None of these are required by us, we only need "isEmpty" from the Collection interface 
	public int size() {
		throw new UnsupportedOperationException();
	}

	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	public Iterator iterator() {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	public Object[] toArray(Object[] a) {
		throw new UnsupportedOperationException();
	}

	public boolean add(Object e) {
		throw new UnsupportedOperationException();
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection c) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}
}
