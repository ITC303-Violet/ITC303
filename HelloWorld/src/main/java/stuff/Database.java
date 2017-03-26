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
		if(c != null) return true;
		try
		{
			Class.forName("org.postgresql.Driver");
			c = DriverManager
			           .getConnection("jdbc:postgresql://localhost:5432/",
			           "postgres", "kittykat");
			Statement statement = c.createStatement();
			statement.executeUpdate("IF NOT EXISTS ( SELECT [name] FROM sys.databases"
					+ " WHERE [name] = 'testdb' ) CREATE DATABASE testdb;"
					+ " IF NOT EXISTS ( SELECT [name] FROM testdb.tables WHERE "
					+ "[name] = 'people' ) CREATE TABLE people ( name varchar(255) );"
					+ "ALTER TABLE people ADD id int NOT NULL IDENTITY (1,1),"
					+ " ADD CONSTRAINT PK_people PRIMARY KEY CLUSTERED (id)");
			c = DriverManager
			           .getConnection("jdbc:postgresql://localhost:5432/testdb",
			           "postgres", "kittykat");
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
				ResultSet results = statement.executeQuery("SELECT * from testdb.people");
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


}
