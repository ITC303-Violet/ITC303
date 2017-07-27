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
		return new ArrayList<Genre>(Genre.getGenres(FactoryManager.getEM()));
	}
	
	List<Genre> genres;
	
	String[] selectedGenres;
	
	public String[] getGenres() {
		if(selectedGenres == null) {
			List<Genre> genres = userBean.getUser().getFavouredGenres();
			selectedGenres = new String[genres.size()];
			for(int i=0; i<genres.size(); i++)
				selectedGenres[i] = genres.get(i).getIdentifier();
		}
		
		return selectedGenres;
	}
	
	public void setGenres(String[] genres) {
		selectedGenres = genres;
	}
	
	private void setUserGenres() {
		List<Genre> validGenres = getGenreChoices();
		Map<String, Genre> genreMap = new HashMap<>();
		for(Genre genre : validGenres)
			genreMap.put(genre.getIdentifier(), FactoryManager.getCommonEM().merge(genre));
		
		List<Genre> userGenres = new ArrayList<Genre>();
		for(int i=0; i<selectedGenres.length; i++)
			if(genreMap.containsKey(selectedGenres[i]))
				userGenres.add(genreMap.get(selectedGenres[i]));
		
		userBean.getUser().setFavouredGenresList(userGenres);
	}
	
	public String save() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		FactoryManager.pullTransaction();
		userBean.setUser(FactoryManager.getCommonEM().merge(userBean.getUser()));
		userBean.getUser().setEmail(email);
		setUserGenres();
		FactoryManager.popTransaction();
		
		context.addMessage(null, new FacesMessage("Your preferences have been saved!"));
		
		return null;
	}
}
