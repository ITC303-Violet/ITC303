package stuff;

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
	List<Person> people;

	public Database()
	{

	}

	private boolean connect()
	{
		try {
			if(c != null && !c.isClosed()) return true;
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try
		{
			Class.forName("org.postgresql.Driver");
			c = DriverManager
			           .getConnection("jdbc:postgresql://localhost:5432/",
			           "postgres", "password");
			Statement statement = c.createStatement();
			ResultSet results = statement.executeQuery("select count(*) from pg_catalog.pg_database where datname = 'testdb'");
			results.next();
			if(results.getInt("count") == 0)
			{
				statement.executeUpdate("CREATE DATABASE testdb");
				c = DriverManager
				           .getConnection("jdbc:postgresql://localhost:5432/testdb",
				           "postgres", "password");
				statement = c.createStatement();
				statement.executeUpdate("CREATE TABLE IF NOT EXISTS people ( id BIGSERIAL PRIMARY KEY, name varchar(255) );");
			}
			else
			{
				c = DriverManager
				           .getConnection("jdbc:postgresql://localhost:5432/testdb",
				           "postgres", "password");
			}

			statement.close();
			return true;
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	private void disconnect()
	{
		if(c == null) return;
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<Person> getPeopleList()
	{
		List<Person> people = new ArrayList<Person>();
		Statement statement = null;
		if(connect())
		{
			try
			{
				statement = c.createStatement();
				ResultSet results = statement.executeQuery("SELECT * from people");
				while(results.next())
				{
					int id = results.getInt("id");
					String name = results.getString("name");
					people.add(new Person(id, name));
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					statement.close();
					disconnect();
					return people;
				}
				catch (SQLException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	public boolean isConnected()
	{
		if(c == null)
		{
			return false;
		}
		return true;
	}

	public void addName(String name)
	{
		Statement statement = null;
		if(connect())
		{
			try
			{
				statement = c.createStatement();
				statement.executeUpdate("INSERT INTO people (name) VALUES ('"+name+"')");
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			finally
			{
				try {
					statement.close();
					disconnect();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}


}
