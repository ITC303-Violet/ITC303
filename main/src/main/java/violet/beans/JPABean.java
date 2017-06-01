package violet.beans;

import java.util.Collections;
import java.util.List;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
//import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.User;

@ManagedBean(name="jpaBean", eager=true)
@ApplicationScoped
public class JPABean {
	//private EntityManagerFactory emf;
	
	public EntityManagerFactory getEMF() {
		return FactoryManager.get();
		/*if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;*/
	}
	
	public List<Game> getGames(int length) {
		EntityManager em = getEMF().createEntityManager();
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g ORDER BY g.release DESC, g.id ASC", Game.class);
			List<Game> result = tq.setMaxResults(length).getResultList();
			return result; // We need to grab it first, or the finally below will close the em before we get the row
		} catch(NoResultException e) {
			return Collections.<Game>emptyList();
		} finally {
			em.close();
		}
	}
	
	public Game getGame(Long id) {
		EntityManager em = getEMF().createEntityManager();
		try {
			TypedQuery<Game> tq = em.createQuery("SELECT g FROM Game g WHERE g.id=:id", Game.class);
			Game result = tq.setParameter("id", id).getSingleResult(); 
			return result;
		} catch(NoResultException e) {
			return null;
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
