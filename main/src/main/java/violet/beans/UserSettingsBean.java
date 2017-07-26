package violet.beans;

import java.util.Collection;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

import javax.faces.bean.ManagedProperty;

import violet.jpa.FactoryManager;
import violet.jpa.Genre;
import violet.jpa.User;

/**
 * Handles setting user's preferences
 * @author somer
 */
@ManagedBean
@RequestScoped
public class UserSettingsBean {
	@ManagedProperty(value = "#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;
	
	public JPABean getJpaBean() {
		return jpaBean;
	}

	public void setJpaBean(JPABean jpaBean) {
		this.jpaBean = jpaBean;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
	
	private String email;
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		return email;
	}
	
	public Collection<Genre> getGenres() {
		return Genre.getGenres(FactoryManager.getEM());
	}
	
	public String save() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		return null;
	}
}
