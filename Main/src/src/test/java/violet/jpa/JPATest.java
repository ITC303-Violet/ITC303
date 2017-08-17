package violet.jpa;

import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;

/**
 * Base class used for general JPA testing
 * @author somer
 */
public class JPATest {
	protected static EntityManager em;
	
	@BeforeClass
	public static void init() {
		FactoryManager.setTesting(true); // ensure the EMF returned is based on the test persistence.xml
		em = FactoryManager.pullCommonEM();
	}
	
	protected void wipeTable(String tableName) {
		try {
			em.createQuery("DELETE FROM " + tableName).executeUpdate();
		} catch(Exception e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to clear table " + tableName);
		}
	}
	
	protected void wipeTables() {
		EntityTransaction t = em.getTransaction();
		try {
			t.begin();
			
			wipeTable("Rating");
			wipeTable("Screenshot");
			wipeTable("Characteristic");
			wipeTable("Genre");
			wipeTable("Game");
			wipeTable("User");
			
			t.commit();
		} catch(RollbackException e) {
			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Failed to clear tables", e);
		}
	}
	
	protected Game createTestGame(int n) {
		Game g = new Game("Game " + n);
		g.setDescription("This is a test game");
		g.setShortDescription("This is a test game");
		g.setRelease(new Date(0));
	
		return g;
	}
	
	protected User createTestUser(int n) {
		return new User("TestUser" + n, "test" + n + "@test.com", "asd123");
	}
	
	protected void initializeTestData() {
		FactoryManager.pullTransaction();
		
		Game g1 = createTestGame(1);
		em.persist(g1);
		
		Genre action = new Genre("Action");
		em.persist(action);
		
		Characteristic graphics = new Characteristic("Graphics");
		em.persist(graphics);
		
		action.addCharacteristic(graphics);
		g1.addGenre(action);
		
		User u1 = createTestUser(1);
		User u2 = createTestUser(2);
		
		em.persist(u1);
		em.persist(u2);
		
		FactoryManager.popTransaction();
		
		u1.rateGame(g1, null, 8.0D);
		u1.rateGame(g1, graphics, 6.0D);
		
		u2.rateGame(g1, null, 4.0D);
		u2.rateGame(g1, graphics, 5.0D);
	}
	
	@Before
	public void initializeDatabase() {
		wipeTables();
		initializeTestData();
	}
	
	@AfterClass
	public static void tearDown() {
		em.clear();
		FactoryManager.popCommonEM();
	}
}
