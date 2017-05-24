package beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

import jpa.User;

@ManagedBean
@RequestScoped
public class SignUpBean extends SignInBean {
	private String email;
	
	private String validationError;
	
	public String getValidationError() {
		return validationError;
	}
	
	public void setValidationError(String validationError) {
		this.validationError = validationError;
	}
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String signUp() {
		if(getJpaBean().findUsername(getUsername()) != null) {
			validationError = "Username is already taken";
			return null;
		}
		
		if(getJpaBean().findUserEmail(email) != null) {
			validationError = "User with that email already exists";
			return null;
		}
		
		User newUser = new User(getUsername(), getEmail(), getPassword());
		createUser(newUser);
		getUserBean().setUser(newUser);
		
		return null;
	}
}
