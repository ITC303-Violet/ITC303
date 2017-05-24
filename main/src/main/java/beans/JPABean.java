package beans;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import jpa.Game;
import jpa.User;

@ManagedBean(name="jpaBean")
@ApplicationScoped
public class JPABean {
	private EntityManagerFactory emf;
	
	public EntityManagerFactory getEMF() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;
	}
	
	public List<Game> getGames(int length) {
		EntityManager em = getEMF().createEntityManager();
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g ORDER BY id ASC LIMIT :limit", Game.class);
			List<Game> result = tq.setParameter("limit", length).getResultList();
			return result; // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			return Collections.<Game>emptyList();
		} finally {
			em.close();
		}
	}
	
	public User findUsername(String username) {
		EntityManager em = getEMF().createEntityManager();
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
	
	public User findUserEmail(String email) {
		EntityManager em = getEMF().createEntityManager();
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
}
