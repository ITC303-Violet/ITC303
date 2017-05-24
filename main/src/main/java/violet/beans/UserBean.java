package violet.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import violet.jpa.User;

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
}
