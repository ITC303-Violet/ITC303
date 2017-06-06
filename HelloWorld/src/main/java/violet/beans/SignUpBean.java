package violet.beans;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import violet.jpa.User;

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
	
	public String signUp() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		if(getJpaBean().findUsername(getUsername()) != null) {
			context.addMessage(null, new FacesMessage("Username is already taken"));
			return null;
		}
		
		if(getJpaBean().findUserEmail(email) != null) {
			context.addMessage(null, new FacesMessage("User with that email already exists"));
			return null;
		}
		
		User newUser = new User(getUsername(), getEmail(), getPassword());
		createUser(newUser);
		getUserBean().setUser(newUser);
		
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try {
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
