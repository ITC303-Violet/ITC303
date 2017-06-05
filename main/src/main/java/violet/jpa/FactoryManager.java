package violet.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Singleton to get an instance of EntityManagerFactory
 * @author somer
 */
public class FactoryManager {
	private static EntityManagerFactory emf;
	private static EntityManager em;
	
	public static EntityManagerFactory get() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;
	}
	
	public static EntityManager getCommonEM() {
		if(em == null || !em.isOpen())
			em = get().createEntityManager();
		
		return em;
	}
}
