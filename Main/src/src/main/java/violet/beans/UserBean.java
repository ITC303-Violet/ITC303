package violet.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import violet.jpa.User;

/**
 * Keeps track of the signed in user
 * @author somer
 */
@ManagedBean
@SessionScoped
public class UserBean {
	private User user = null;
	
	public User getUser() {
		return user;
	}
	
	public void setUser(User user) {
		this.user = user;
	}
	
	/**
	 * 
	 * @return true if the user is signed in and has "isStaff" set to true
	 */
	public boolean isStaff() {
		return user != null && user.getIsStaff();
	}
	
	/**
	 * @return true if a user is signed in
	 */
	public boolean isAuthenticated() {
		return user != null;
	}
}
