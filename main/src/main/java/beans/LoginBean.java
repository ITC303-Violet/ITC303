package main.java.beans;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import main.java.jpa.User;

@ManagedBean
@RequestScoped
public class LoginBean {
	@ManagedProperty(value="#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value="#{userBean}")
	private UserBean userBean;
	
	private String username;
	private String password;
	private String email;
	
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
	
	public String getEmail() {
		return email;
	}
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	private User findUsername(String username) {
		EntityManager em = jpaBean.getEMF().createEntityManager();
		try {
			TypedQuery<User> tq = em.createQuery("SELECT u FROM User u WHERE LOWER(u.username)=:username", User.class);
			User result = tq.setParameter("username", username.toLowerCase()).getSingleResult(); 
			return result; // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}
	
	private User findUserEmail(String email) {
		EntityManager em = jpaBean.getEMF().createEntityManager();
		try {
			TypedQuery<User> tq = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email)=:email", User.class);
			User result = tq.setParameter("email", email.toLowerCase()).getSingleResult();
			return result;
		} catch(NoResultException e) {
			return null;
		} finally {
			em.close();
		}
	}
	
	private boolean createUser(User user) {
		EntityManager em = jpaBean.getEMF().createEntityManager();
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
		User checkUser = findUsername(username);
		if(checkUser == null) {
			validationError = "Incorrect username or password";
			return null;
		}
		
		if(!checkUser.checkPassword(password)) {
			validationError = "Incorrect username or password";
			return null;
		}
		
		userBean.setUser(checkUser);
		
		return null;
	}
	
	public String signUp() {
		if(findUsername(username) != null) {
			validationError = "Username is already taken";
			return null;
		}
		
		if(findUserEmail(email) != null) {
			validationError = "User with that email already exists";
			return null;
		}
		
		User newUser = new User(username, email, password);
		createUser(newUser);
		userBean.setUser(newUser);
		
		return null;
	}
	
	public String signOut() {
		userBean.setUser(null);
		
		return null;
	}
}
