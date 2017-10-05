package violet.controllers.xml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
/**
 * Since Nintendo samurai servers return only 50 titles by request, 
 * this class helps reading them by looping through the 
 * total number of titles found in the 'contents' returned in the 
 * XML data.
 * 
 * Performance still should be tested.
 * 
 * @author Erin
 */
public abstract class NintendoListReader {
	
	private static final String URL_APPLIST = "https://samurai.ctr.shop.nintendo.net/samurai/ws/US/titles/?shop_id=2";
	private int totalTitles;
	
	/**
	 * Called when all of the titles have been read from the server.
	 * @param full list of titles
	 */
	public abstract void onTitlesLoaded(List<XMLTag> list);
	/**
	 * Sets the total titles number. Also, it starts looping
	 * through it increasing 50 each time, if it's != 0
	 * 
	 * @param integer number of titles
	 */
	private void setTotalTitles(int totalTitles) throws IOException {
		this.totalTitles=totalTitles;
		if(totalTitles>0) {
			List<XMLTag> fullTitles=new ArrayList<XMLTag>();
			for(int i=0;i<totalTitles;i+=50) {
				
				XMLReader<XMLTag> reader=new XMLReader<XMLTag>(URL_APPLIST+"&offset="+i,"content") {

					@Override
					public XMLTag parseObject(XMLTag mainTag) {
						return mainTag;
					}};
					
					fullTitles.addAll(reader.readElementList());
					
			}
			onTitlesLoaded(fullTitles);
		}
		
	}
	private int getTotalTitles() {
		return this.totalTitles;
	}
	public NintendoListReader() {
		
				
	    
		
	}
	/**
	 * Starts reading the 'contents' tag so we now the total number of titles
	 * present in the server.
	 * 
	 */
	public void query() throws IOException {
		XMLReader.readSingleTag(URL_APPLIST, "contents", new XMLReader.SingleTagReadCallback() {
			
			@Override
			public void onSingleRead(XMLTag tag) {

				try {
					setTotalTitles(Integer.parseInt(tag.getAttribute("total").getValue()));
				} catch (NumberFormatException e) {

					e.printStackTrace();
				} catch (IOException e) {

					e.printStackTrace();
				}
			}
		});
	}
}
