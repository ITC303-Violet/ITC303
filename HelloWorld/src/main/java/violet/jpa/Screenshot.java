package violet.jpa;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Column;

@Entity
public class Screenshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@AttributeOverrides({@AttributeOverride(name="filename", column=@Column(name="thumbnail_filename"))})
	@Embedded
	private Image thumbnail;
	
	@AttributeOverrides({@AttributeOverride(name="filename", column=@Column(name="image_filename"))})
	@Embedded
	private Image image;
	
	private String remoteIdentifier;
	
	@ManyToOne
	private Game game;
	
	public Screenshot() {
		
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Image getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(Image thumbnail) {
		this.thumbnail = thumbnail;
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public String getRemoteIdentifier() {
		return remoteIdentifier;
	}

	public void setRemoteIdentifier(String remoteIdentifier) {
		this.remoteIdentifier = remoteIdentifier;
	}
	
	public Game getGame() {
		return game;
	}
	
	public void setGame(Game game) {
		this.game = game;
	}
}
