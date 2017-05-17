package stuff;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Database
{
	Connection c = null;

	//local values, will be reset in constructor if prod
	private static String PASSWORD = "password";
	private static String USER = "postgres";
	private static String URL = "jdbc:postgresql://localhost:5432/";

	public Database()
	{
		if(Global.PROD)
		{
			URI dbUri = null;
			try {
				dbUri = new URI(System.getenv("DATABASE_URL"));
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println(e.getMessage());
			}
			PASSWORD = dbUri.getUserInfo().split(":")[1];
			USER = dbUri.getUserInfo().split(":")[0];
			URL = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath();
			//URL = "jdbc:postgresql://ec2-54-83-47-194.compute-1.amazonaws.com:5432/";
		}

	}

	public String getPassword()
	{
		return PASSWORD;
	}
	public String getUser()
	{
		return USER;
	}
	public String getURL()
	{
		return URL;
	}

	private boolean connect()
	{
		if(Global.PROD)
		{
			try
			{
				if(c != null && !c.isClosed()) return true;
				c = DriverManager.getConnection(URL, USER, PASSWORD);
				Statement statement = c.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS users ( username varchar(255) PRIMARY KEY, password varchar(255));");
				statement.close();
				return true;
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				return false;
			}
		}
		else
		{

			try {
				if(c != null && !c.isClosed()) return true;
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				//e1.printStackTrace();
				System.out.println(e1.getMessage());
			}
			try
			{
				Class.forName("org.postgresql.Driver");
				c = DriverManager.getConnection(URL + "testdb", USER, PASSWORD);
				Statement statement = c.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS users ( username varchar(255) PRIMARY KEY, password varchar(255));");

				statement.close();
			return true;
			}
			catch (Exception e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println(e.getMessage());
			}
			return false;
		}
	}
	private void disconnect()
	{
		if(c == null) return;
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}


	public boolean isConnected()
	{
		if(c == null)
		{
			return false;
		}
		return true;
	}

	public String getPassword(String username)
	{
		Statement statement = null;
		String password = "";
		if(connect())
		{
			try
			{
				statement = c.createStatement();
				ResultSet rs = statement.executeQuery("SELECT * FROM users WHERE username = '"+username+"'");
				while(rs.next())
				{
					password = rs.getString("password");
				}
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				System.out.println(e.getMessage());
			}
			finally
			{
				try {
					statement.close();
					disconnect();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.out.println(e.getMessage());
				}
			}
		}
		return password;
	}

	public void addUser(String username, String password)
	{
		Statement statement = null;
		if(connect())
		{
			try
			{
				statement = c.createStatement();
				statement.executeUpdate("INSERT INTO users (username, password) VALUES ('"+username+"', '"+password+"')");
			}
			catch(Exception e)
			{
				//e.printStackTrace();
				System.out.println(e.getMessage());
				//maybe error here is user already exists - should capture that and send back 'false' or something
			}
			finally
			{
				try {
					statement.close();
					disconnect();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					System.out.println(e.getMessage());
				}
			}
		}
	}
}
