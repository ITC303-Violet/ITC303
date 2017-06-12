package violet.jpa;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

/**
 * Singleton to get an instance of EntityManagerFactory
 * @author somer
 */
public class FactoryManager {
	private static EntityManagerFactory emf;
	private static boolean testing = false;
	
	private static ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();
	private static ThreadLocal<Integer> entityManagerDepth =
			new ThreadLocal<Integer>() {
				@Override protected Integer initialValue() {
					return 0;
				}
	};
	
	private static ThreadLocal<Integer> transactionDepth =
			new ThreadLocal<Integer>() {
				@Override protected Integer initialValue() {
					return 0;
				}
	};
	
	public static void setTesting(boolean testing) {
		FactoryManager.testing = testing;
		if(emf != null && emf.isOpen())
			emf.close();
		emf = null;
	}
	
	public static EntityManagerFactory get() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory(testing ? "test" : "default");
		
		return emf;
	}
	
	public static EntityManager getEM() {
		return get().createEntityManager();
	}
	
	public static EntityManager getCommonEM() {
		return entityManager.get();
	}
	
	public static EntityManager pullCommonEM() {
		if(entityManager.get() == null || !entityManager.get().isOpen())
			entityManager.set(getEM());
		
		entityManagerDepth.set(entityManagerDepth.get() + 1);
		
		return entityManager.get();
	}
	
	public static void popCommonEM() {
		if(entityManager.get() == null || !entityManager.get().isOpen())
			entityManager.set(getEM());
		
		Integer i = entityManagerDepth.get();
		if(i <= 1 && entityManager.get().isOpen())
			entityManager.get().close();
		
		entityManagerDepth.set(i - 1);
	}
	
	public static EntityTransaction reopenTransaction() {
		EntityTransaction et = getCommonEM().getTransaction();
		if(et.isActive()) {
			et.commit();
			et.begin();
		}
		
		return et;
	}
	
	public static EntityTransaction pullTransaction() {
		EntityTransaction et = pullCommonEM().getTransaction();
		Integer i = transactionDepth.get();
		/*if(!et.isActive())
			i = 0;*/
		if(i == 0 && !et.isActive())
			et.begin();
		
		transactionDepth.set(i + 1);
		
		return et;
	}
	
	public static void popTransaction() {
		Integer i = transactionDepth.get();
		if(i <= 0)
			return;
		
		EntityTransaction et = getCommonEM().getTransaction();
		if(i <= 1 && et.isActive())
			et.commit();
		
		popCommonEM();
		
		transactionDepth.set(i - 1);
	}
}
