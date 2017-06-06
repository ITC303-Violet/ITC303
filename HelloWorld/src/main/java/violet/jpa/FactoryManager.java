package violet.jpa;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class FactoryManager {
	private static EntityManagerFactory emf;
	
	public static EntityManagerFactory get() {
		if(emf == null)
			emf = Persistence.createEntityManagerFactory("default");
		
		return emf;
	}
}
