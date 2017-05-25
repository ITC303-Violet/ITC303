package violet.beans;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import violet.jpa.User;

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

	public String signIn() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		User checkUser = getJpaBean().findUsername(getUsername());
		if(checkUser == null || !checkUser.checkPassword(getPassword())) {
			context.addMessage(null, new FacesMessage("Incorrect username or password"));
			return null;
		}

		getUserBean().setUser(checkUser);
		
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try {
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public String signOut() {
		getUserBean().setUser(null);
		
		FacesContext context = FacesContext.getCurrentInstance();
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
