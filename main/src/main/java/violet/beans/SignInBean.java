package violet.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;

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

	private String validationError;

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

	public String getValidationError() {
		return validationError;
	}

	public void setValidationError(String validationError) {
		this.validationError = validationError;
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
		} catch (Exception e) {
			return false;
		} finally {
			em.close();
		}

		return true;
	}

	public String signIn() {
		User checkUser = getJpaBean().findUsername(getUsername());
		if (checkUser == null) {
			validationError = "Incorrect username or password";
			return null;
		}

		if (!checkUser.checkPassword(getPassword())) {
			validationError = "Incorrect username or password";
			return null;
		}

		getUserBean().setUser(checkUser);

		return null;
	}

	public String signOut() {
		getUserBean().setUser(null);

		return null;
	}
}
