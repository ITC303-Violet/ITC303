package stuff;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped //this means that each individual http session will have one of these i.e. one per user per log in session
public class UserBean
{
	@ManagedProperty(value="#{master}")
    private Master master; //this is so it can ask Master to do things for it

	private String name;
	private String password; //these are the ones entered in the log in section so we can check them
	private String loggedIn; //replace this with a useful object to have when a user is logged in
	private String errorMessage;

	@PostConstruct
	public void initialise()
	{
		name = "";
		password = "";
		errorMessage = "";
	}

	public Master getMaster() {
		return master;
	}

	public void setMaster(Master master) {
		this.master = master;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(String loggedIn) {
		this.loggedIn = loggedIn;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public void startSession()
	{
		if(loggedIn != null)
		{
			errorMessage = "You are already logged in this session. Please go here (this will be a link to a url to the page they get sent to once logged in).";
			return;
		}
		if(name.trim().equals("") || password.trim().equals(""))
		{
			errorMessage = "Neither user name nor password can be blank";
			return;
		}
		loggedIn = master.logIn(this);
		if(loggedIn == null)
		{
			errorMessage = "Wrong password for existing user. Please try again";
			return;
		}
		errorMessage = "";
	}
}
