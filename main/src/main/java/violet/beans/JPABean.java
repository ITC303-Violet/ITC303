package violet.beans;

import java.util.Collections;
import java.util.List;

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
import violet.jpa.Paginator;
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
	 * Returns a {@link Paginator} object containing the list of games
	 * @param page the page number
	 * @param length length of a page (i.e. 25 games)
	 * @param releasedOnly if true, only returns games that have a release date before the current date
	 * @return a {@link Paginator} object containing the list of games
	 */
	public Paginator<Game> getPaginatedGames(int page, int length, boolean releasedOnly) {
		EntityManager em = getEM();
		try {
			String queryStart = "SELECT g FROM Game g WHERE g.blacklisted=FALSE";
			if(releasedOnly)
				queryStart += "  AND g.release < CURRENT_TIMESTAMP";
			TypedQuery<Game> tq = em.createQuery(queryStart + " ORDER BY g.release DESC NULLS LAST, g.id ASC", Game.class);
			List<Game> list = tq
					.setFirstResult((page-1) * length)
					.setMaxResults(length)
					.getResultList();
			Long count = em.createQuery("SELECT COUNT(g) FROM Game g WHERE g.blacklisted=FALSE", Long.class).getSingleResult();
			return new Paginator<Game>(page, length, (int)(count/length+1), list); // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			List<Game> list = Collections.<Game>emptyList();
			return new Paginator<Game>(page, length, 0, list);
		}
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
