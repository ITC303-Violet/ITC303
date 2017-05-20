package main.java.jpa;

import javax.persistence.Embeddable;

@Embeddable
public class RatingKey {
	private Long userId;
	private Long gameId;
	private Long characteristicId;
	
	public RatingKey() {
		
	}
	
	public Long getUserId() {
		return userId;
	}
	
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getGameId() {
		return gameId;
	}
	
	public void setGameId(Long gameId) {
		this.gameId = gameId;
	}
	
	public Long getCharacteristicId() {
		return characteristicId;
	}
	
	public void setCharacteristicId(Long characteristicId) {
		this.characteristicId = characteristicId;
	}
}