package stuff;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database
{
	Connection c = null;

	public Database()
	{

	}

	public void connect()
	{
		if(c != null) return;
		try
		{
			Class.forName("org.postgresql.Driver");
			c = DriverManager
			           .getConnection("jdbc:postgresql://localhost:5432/",
			           "postgres", "password");
			Statement statement = c.createStatement();
			statement.executeUpdate("IF EXISTS ( SELECT [name] FROM sys.databases"
					+ " WHERE [name] = 'testdb' ) DROP DATABASE testdb;"
					+ " CREATE DATABASE testdb");
			c = DriverManager
			           .getConnection("jdbc:postgresql://localhost:5432/testdb",
			           "postgres", "password");
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
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


}
