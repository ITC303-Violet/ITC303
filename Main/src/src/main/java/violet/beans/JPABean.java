package violet.beans;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.User;

/**
 * Provides easy access for other beans to the JPA entity manager and some methods to perform queries
 * @author somer
 */
@ManagedBean(name="jpaBean", eager=true)
@RequestScoped
public class JPABean {
	public static class JPAEquippedBean {
		@ManagedProperty(value = "#{jpaBean}")
		private JPABean jpaBean;
		
		public JPABean getJpaBean() {
			return jpaBean;
		}

		public void setJpaBean(JPABean jpaBean) {
			this.jpaBean = jpaBean;
		}
	}
	
	public EntityManager getEM() {
		return FactoryManager.getCommonEM();
	}
	
	@PostConstruct
	public void initialized() {
		FactoryManager.pullCommonEM();
	}
	
	@PreDestroy
	public void destroy() {
		FactoryManager.popCommonEM();
	}
	
	/**
	 * Get a game based on the id
	 * @param id
	 * @return the game with id or null
	 */
	public Game getGame(Long id) {
		EntityManager em = getEM();
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g WHERE g.id=:id AND g.blacklisted=FALSE", Game.class);
			Game result = tq.setParameter("id", id).getSingleResult(); 
			return result;
		} catch(NoResultException e) {
			return null;
		}
	}
	
	/**
	 * @param username
	 * @return user with username or null
	 */
	public User findUsername(String username) {
		EntityManager em = getEM();
		try {
			TypedQuery<User> tq = em.createQuery("SELECT u FROM User u WHERE LOWER(u.username)=:username", User.class);
			User result = tq.setParameter("username", username.toLowerCase()).getSingleResult(); 
			return result; // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			return null;
		}
	}
	
	/**
	 * @param email
	 * @return user with email or null
	 */
	public User findUserEmail(String email) {
		EntityManager em = getEM();
		try {
			TypedQuery<User> tq = em.createQuery("SELECT u FROM User u WHERE LOWER(u.email)=:email", User.class);
			User result = tq.setParameter("email", email.toLowerCase()).getSingleResult();
			return result;
		} catch(NoResultException e) {
			return null;
		}
	}
}
