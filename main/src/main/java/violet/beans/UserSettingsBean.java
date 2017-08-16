package violet.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.bean.ManagedProperty;

import violet.jpa.Characteristic;
import violet.jpa.FactoryManager;
import violet.jpa.Genre;

/**
 * Handles setting user's preferences
 * @author somer
 */
@ManagedBean
@RequestScoped
public class UserSettingsBean {
	@ManagedProperty(value = "#{jpaBean}")
	private JPABean jpaBean;

	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;
	
	public JPABean getJpaBean() {
		return jpaBean;
	}

	public void setJpaBean(JPABean jpaBean) {
		this.jpaBean = jpaBean;
	}

	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
	
	private String email;
	
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getEmail() {
		if(email == null)
			email = userBean.getUser().getEmail();
		
		return email;
	}
	
	public List<Genre> getGenreChoices() {
		return new ArrayList<Genre>(Genre.getGenres(FactoryManager.getCommonEM()));
	}
	
	private String[] genres;
	
	public String[] getGenres() {
		if(genres == null) {
			List<Genre> currentGenres = userBean.getUser().getFavouredGenres();
			genres = new String[currentGenres.size()];
			for(int i=0; i<currentGenres.size(); i++)
				genres[i] = currentGenres.get(i).getIdentifier();
		}
		
		return genres;
	}
	
	public void setGenres(String[] genres) {
		this.genres = genres;
	}
	
	private void setUserGenres() {
		List<Genre> validGenres = getGenreChoices();
		Map<String, Genre> genreMap = new HashMap<>();
		for(Genre genre : validGenres)
			genreMap.put(genre.getIdentifier(), FactoryManager.getCommonEM().merge(genre));
		
		List<Genre> userGenres = new ArrayList<Genre>();
		for(int i=0; i<genres.length; i++)
			if(genreMap.containsKey(genres[i]))
				userGenres.add(genreMap.get(genres[i]));
		
		userBean.getUser().setFavouredGenresList(userGenres);
	}
	
	
	public List<Characteristic> getCharacteristicChoices() {
		return new ArrayList<Characteristic>(Characteristic.getCharacteristics(FactoryManager.getCommonEM(), false));
	}
	
	private Long[] characteristics;
	
	public Long[] getCharacteristics() {
		if(characteristics == null) {
			List<Characteristic> currentCharacteristics = userBean.getUser().getFavouredCharacteristics();
			characteristics = new Long[currentCharacteristics.size()];
			for(int i=0; i<currentCharacteristics.size(); i++)
				characteristics[i] = currentCharacteristics.get(i).getId();
		}
		
		return characteristics;
	}
	
	public void setCharacteristics(Long[] characteristics) {
		this.characteristics = characteristics;
	}
	
	private void setUserCharacteristics() {
		List<Characteristic> validCharacteristics = getCharacteristicChoices();
		Map<Long, Characteristic> characteristicMap = new HashMap<>();
		for(Characteristic characteristic : validCharacteristics)
			characteristicMap.put(characteristic.getId(), FactoryManager.getCommonEM().merge(characteristic));
		
		List<Characteristic> userCharacteristics = new ArrayList<>();
		for(int i=0; i<characteristics.length; i++)
			if(characteristicMap.containsKey(characteristics[i]))
				userCharacteristics.add(characteristicMap.get(characteristics[i]));
		
		userBean.getUser().setFavouredCharacteristicsList(userCharacteristics);
	}
	
	
	public String save() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		FactoryManager.pullTransaction();
		userBean.setUser(FactoryManager.getCommonEM().merge(userBean.getUser()));
		userBean.getUser().setEmail(email);
		setUserGenres();
		setUserCharacteristics();
		FactoryManager.popTransaction();
		
		context.addMessage(null, new FacesMessage("Your preferences have been saved!"));
		
		return null;
	}
}
