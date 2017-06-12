package violet.jpa;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JPARatingAverageTest extends JPATest {
	public void checkAverage(Game game, Characteristic characteristic, Double average) {
		Rating rating = game.getAverageCharacteristicRating(characteristic, em);
		
		assertNotNull(rating);
		assertEquals(average, rating.getRating(), 0.01D);
	}
	
	@Test
	public void checkAverages() {
		Game game = em.createQuery("SELECT g FROM Game g", Game.class).setMaxResults(1).getSingleResult();
		Characteristic characteristic = game.getCharacteristics().get(0);
		
		checkAverage(game, null, 6.0D);
		checkAverage(game, characteristic, 5.5D);
		
		FactoryManager.pullTransaction();
		User u3 = createTestUser(3);
		em.persist(u3);
		FactoryManager.popTransaction();
		
		u3.rateGame(game, null, 10.0D);
		u3.rateGame(game,  characteristic, 10.0D);
		
		checkAverage(game, null, 8.0D);
		checkAverage(game, characteristic, 7.75D);
		
		u3.rateGame(game, null, 2.0D);
		u3.rateGame(game,  characteristic, 3.0D);
		
		checkAverage(game, null, 4.0D);
		checkAverage(game, characteristic, 4.25D);
	}
}
