package violet.beans;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import violet.jpa.User;

/**
 * Handles users signing in
 * @author somer
 */
@ManagedBean
@RequestScoped
public class SignInBean {
	@ManagedProperty(value = "#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;

	private String username;
	private String password;

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

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * Persists a user object in the database
	 * @param user
	 * @return true if the user is persisted successfully
	 */
	protected boolean createUser(User user) {
		EntityManager em = getJpaBean().getEMF().createEntityManager();
		try {
			em.getTransaction().begin();
			em.persist(user);
			em.getTransaction().commit();
		} catch(Exception e) {
			return false;
		} finally {
			em.close();
		}

		return true;
	}

	/**
	 * JSF action to sign a user in
	 * @return
	 */
	public String signIn() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		// check the user exists, if so, check the password is correct
		User checkUser = getJpaBean().findUsername(getUsername());
		if(checkUser == null || !checkUser.checkPassword(getPassword())) {
			context.addMessage(null, new FacesMessage("Incorrect username or password"));
			return null;
		}

		getUserBean().setUser(checkUser);
		
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try { // Reload the page to ensure the page data is correct and up to date
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * JSF action to sign a user out
	 * @return
	 */
	public String signOut() {
		getUserBean().setUser(null); // unset the user
		
		FacesContext context = FacesContext.getCurrentInstance();
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try { // reload the page to ensure data is up to date
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
