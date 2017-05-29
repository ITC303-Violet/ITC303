package stuff;

import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

@ManagedBean
@ApplicationScoped //this means there will only be one of the lifetime of the application
public class Master
{
	private Database db;

	@PostConstruct
	public void initialise()
	{
		db = new Database();
	}

	public String logIn(UserBean user) //replace String with relevant object
	{
		//Does the user exist?
		String password = db.getPassword(user.getName());
		if(password.equals("")) //user doesn't exist, so add them
		{
			db.addUser(user.getName(), user.getPassword());
		}
		else if(!password.equals(user.getPassword())) return null; //password does not match existing user's password
		return "all good";
	}
}
