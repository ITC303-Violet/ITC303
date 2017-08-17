package violet.beans;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import violet.jpa.User;

/**
 * Handles signing up
 * @author somer
 */
@ManagedBean
@RequestScoped
public class SignUpBean extends SignInBean {
	private String email;
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	/**
	 * JSF action to handle user signup
	 * @return
	 */
	public String signUp() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		if(getJpaBean().findUsername(getUsername()) != null) { // check the username isn't already taken
			context.addMessage(null, new FacesMessage("Username is already taken"));
			return null;
		}
		
		if(getJpaBean().findUserEmail(email) != null) { // check a user with the same email address doesn't already exist
			context.addMessage(null, new FacesMessage("User with that email already exists"));
			return null;
		}
		
		// Create the new user with provided details
		User newUser = new User(getUsername(), getEmail(), getPassword());
		createUser(newUser); // save that user
		getUserBean().setUser(newUser); // set the the user (sign the user in immediately)
		
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try { // same as in SignInBean, reload the page to keep everything up to date
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
