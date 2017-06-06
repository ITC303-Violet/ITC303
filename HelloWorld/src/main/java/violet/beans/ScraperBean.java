package violet.beans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.io.Reader;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;

import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import org.primefaces.json.JSONObject;

import violet.jpa.Game;

@ManagedBean
@ApplicationScoped
public class ScraperBean
{
	private List<Game> newSteamGameList;
	private Map<String, Game> steamGameMap;

	@ManagedProperty(value="#{jpaBean}")
	private JPABean jpaBean;

	@PostConstruct
	public void initialise()
	{
		newSteamGameList = new ArrayList<Game>();
		steamGameMap = jpaBean.getGameMap("S");
	}

	public JPABean getJpaBean() {
		return jpaBean;
	}

	public void setJpaBean(JPABean jpaBean) {
		this.jpaBean = jpaBean;
	}

	public List<Game> getNewSteamGameList() {
		return newSteamGameList;
	}

	public void setNewSteamGameList(List<Game> newSteamGameList) {
		this.newSteamGameList = newSteamGameList;
	}

	public void steam()
	{
		newSteamGameList.clear(); //only want new titles
		try
		{
			JSONObject steamJson = jsonFromURL("http://api.steampowered.com/ISteamApps/GetAppList/v0001/");
			JSONArray appsArray = steamJson.getJSONObject("applist").getJSONObject("apps").getJSONArray("app");

			List<Game> tempList = new ArrayList<Game>();
			for(int i = 0; i < appsArray.length(); i++)
			{
	            JSONObject entry = appsArray.getJSONObject(i);
	            String id = Integer.toString(entry.getInt("appid"));
	            if(steamGameMap.get(id) != null) continue; //if we already have it in the database, don't add it

	            //to get more details:
	            JSONObject appDataJson = null;
	            int wait = 1;
	            while(appDataJson == null)
	            {
	            	try
	            	{
	            		appDataJson = jsonFromURL("http://store.steampowered.com/api/appdetails?appids="+id);
	            		wait = 1; //if it gets to here there has been no exception
	            	}
	            	catch(IOException e)//409 too many requests
	            	{
	            		System.out.println(e.getMessage());
	            		appDataJson = null; //in case somehow it did get set
	            		System.out.println("Sleeping for " + wait + " seconds...");
	            		TimeUnit.SECONDS.sleep(wait);
	            		wait = wait * 2;
	            	}
	            	catch(Exception e)
	            	{
	            		e.printStackTrace(); //some other exception
	            	}
	            }
	            if(appDataJson.getJSONObject(String.valueOf(id)).getBoolean("success"))//this is false if the id doesn't exist
	            {
	                JSONObject dataObject = appDataJson.getJSONObject(String.valueOf(id)).getJSONObject("data");
	                String type = dataObject.getString("type");
	                if(!type.equals("game")) continue; //don't care about dlc etc
	                String name = entry.getString("name");
		            Game game = new Game(id, name, "S");
		            tempList.add(game);
		            if(tempList.size() > 4) //this is so titles are periodically added to the DB, and if it crashes then we haven't wasted a bunch of time
		            {
		            	jpaBean.addGames(tempList);
		            	newSteamGameList.addAll(tempList);
		            	tempList.clear();
		            }
	            }
	        }
			jpaBean.addGames(tempList);
        	newSteamGameList.addAll(tempList);
        	tempList.clear();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	  //This method gets the URL data as an input stream, then parses the data as a JSON.
	  public static JSONObject jsonFromURL(String url) throws IOException, JSONException {
	    InputStream input = new URL(url).openStream();
	    try {
	      InputStreamReader isr=
	              new InputStreamReader(input, Charset.forName("UTF-8"));
	      BufferedReader br = new BufferedReader(isr);
	      String jsonString = readerToString(br);
	      JSONObject jsonObj = new JSONObject(jsonString);
	      return jsonObj;
	    } finally {
	      input.close();
	    }
	  }
	    //Parses the received Reader object to a String representation.
	   private static String readerToString(Reader reader) throws IOException {
	    StringBuilder stringBuilder = new StringBuilder();

	    int character;
	    while ((character = reader.read())!=-1) {
	      stringBuilder.append((char) character);
	    }
	    return stringBuilder.toString();
	  }
}
