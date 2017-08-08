package violet.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import violet.gatherers.ScheduledJob;
import violet.jpa.Characteristic;
import violet.jpa.FactoryManager;
import violet.jpa.User;
import violet.jpa.Game;
import violet.jpa.Genre;
import violet.jpa.Rating;

/**
 * Handles administration requests such as triggering an update of the games list
 * @author somer
 */
@ManagedBean
@RequestScoped
public class AdminBean extends JPABean.JPAEquippedBean {
	private int maxGamesGather;
	
	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;
	
	@ManagedProperty(value = "#{quartzBean}")
	private QuartzBean quartzBean;
	
	public int getMaxGamesGather() {
		return maxGamesGather;
	}
	
	public void setMaxGamesGather(int maxGamesGather) {
		this.maxGamesGather = maxGamesGather;
	}
	
	private Long gameCount;
	public Long getGameCount() {
		if(gameCount == null)
			gameCount = Game.count();
		
		return gameCount;
	}
	
	private Long userCount;
	public Long getUserCount() {
		if(userCount == null)
			userCount = User.count();
		
		return userCount;
	}
	
	private Long genreCount;
	public Long getGenreCount() {
		if(genreCount == null)
			genreCount = Genre.count();
		
		return genreCount;
	}
	
	private Long characteristicCount;
	public Long getCharacteristicCount() {
		if(characteristicCount == null)
			characteristicCount = Characteristic.count();
		
		return characteristicCount;
	}
	
	private Long ratingCount;
	public Long getRatingCount() {
		if(ratingCount == null)
			ratingCount = Rating.count();
		
		return ratingCount;
	}
	
	public UserBean getUserBean() {
		return userBean;
	}

	public void setUserBean(UserBean userBean) {
		this.userBean = userBean;
	}
	
	public QuartzBean getQuartzBean() {
		return quartzBean;
	}

	public void setQuartzBean(QuartzBean quartzBean) {
		this.quartzBean = quartzBean;
	}
	
	/**
	 * Triggers an update of the game list
	 * @return
	 */
	public String gather() {
		if(!userBean.isStaff())
			return null;
		
		FacesContext context = FacesContext.getCurrentInstance();
		
		// Build the job to trigger
		JobDetail job = JobBuilder.newJob(ScheduledJob.class)
				.withIdentity("manual-gather", "GATHERERS") // Set the identity so we don't have multiple instances of the job running
				.usingJobData("insert-only", false) // Update games rather than only inserting them
				.usingJobData("max-games", maxGamesGather)
				.build();
		
		// Build the trigger (just trigger immediately)
		Trigger trigger = TriggerBuilder.newTrigger()
				.withIdentity("manual-gather", "GATHERERS")
				.startNow()
				.build();
		
		try {
			Scheduler scheduler = quartzBean.getSchedulerFactory().getScheduler();
			scheduler.scheduleJob(job, trigger);
		} catch(SchedulerException e) {
			e.printStackTrace();
			context.addMessage(null, new FacesMessage("An error occured while scheduling the gathering"));
			return null;
		}
		
		ExternalContext externalContext = context.getExternalContext();
		HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
		
		try {
			// Reload the page so it looks like we're doing something
			// TODO: Possibly add a textbox with the output of the update
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public String saveCharacteristics() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		EntityManager em = FactoryManager.pullCommonEM();
		FactoryManager.pullTransaction();
		try {
			for(Map.Entry<Genre, List<String>> entry : genreCharacteristics.entrySet()) {
				List<Characteristic> setChars = new ArrayList<Characteristic>();
				
				List<String> characteristics = entry.getValue();
				if(characteristics != null)
					for(String characteristicName : characteristics) {
						Characteristic characteristic = Characteristic.getCharacteristic(characteristicName, true, em);
						setChars.add(characteristic);
					}
				
				List<Characteristic> removeChars = new ArrayList<Characteristic>();
				Genre genre = entry.getKey();
				for(Characteristic characteristic : genre.getCharacteristics())
					if(!setChars.contains(characteristic))
						removeChars.add(characteristic);
				
				for(Characteristic characteristic : removeChars)
					genre.removeCharacteristic(characteristic);
				for(Characteristic characteristic : setChars)
					genre.addCharacteristic(characteristic);
			}
		} catch(Exception e) {
			throw e;
		} finally {
			FactoryManager.popTransaction();
			FactoryManager.popCommonEM();
		}
		
		context.addMessage(null, new FacesMessage("Saved characteristics"));
		
		return null;
	}
	
	
	Map<Genre, List<String>> genreCharacteristics;
	
	@PostConstruct
	public void generateGenreCharacteristics() {
		genreCharacteristics = new HashMap<Genre, List<String>>();
		
		for(Genre genre : getGenreChoices()) {
			List<String> characteristics = new ArrayList<String>();
			for(Characteristic characteristic : genre.getCharacteristics())
				characteristics.add(characteristic.getName());
			
			genreCharacteristics.put(genre, characteristics);
		}
	}
	
	
	public Map<Genre, List<String>> getGenreCharacteristics() {
		return genreCharacteristics;
	}
	
	
	public List<Genre> getGenreChoices() {
		return new ArrayList<Genre>(Genre.getGenres(FactoryManager.getCommonEM(), true));
	}
	
	String[] blacklistedGenres;
	
	public String[] getBlacklistedGenres() {
		EntityManager em = FactoryManager.getCommonEM();
		if(blacklistedGenres == null) {
			List<Genre> genres = em.createQuery("SELECT g FROM Genre g WHERE g.blacklisted=TRUE", Genre.class).getResultList();
			blacklistedGenres = new String[genres.size()];
			for(int i=0; i<genres.size(); i++)
				blacklistedGenres[i] = genres.get(i).getIdentifier();
		}
		
		return blacklistedGenres;
	}
	
	public void setBlacklistedGenres(String[] genres) {
		blacklistedGenres = genres;
	}
	
	private void updateBlacklistedGenres() {
		List<Genre> validGenres = getGenreChoices();
		Map<String, Genre> genreMap = new HashMap<>();
		for(Genre genre : validGenres)
			genreMap.put(genre.getIdentifier(), genre);
		
		FactoryManager.pullTransaction();
		try {
			for(int i=0; i<blacklistedGenres.length; i++)
				if(genreMap.containsKey(blacklistedGenres[i])) {
					genreMap.get(blacklistedGenres[i]).setBlacklisted(true);
					genreMap.remove(blacklistedGenres[i]);
				}
			
			for(Genre genre : genreMap.values())
				genre.setBlacklisted(false);
		} finally {
			FactoryManager.popTransaction();
		}
	}
	
	String[] hiddenGenres;
	
	public String[] getHiddenGenres() {
		EntityManager em = FactoryManager.getCommonEM();
		if(hiddenGenres == null) {
			List<Genre> genres = em.createQuery("SELECT g FROM Genre g WHERE g.hidden=TRUE", Genre.class).getResultList();
			hiddenGenres = new String[genres.size()];
			for(int i=0; i<genres.size(); i++)
				hiddenGenres[i] = genres.get(i).getIdentifier();
		}
		
		return hiddenGenres;
	}
	
	public void setHiddenGenres(String[] genres) {
		hiddenGenres = genres;
	}
	
	private void updateHiddenGenres() {
		List<Genre> validGenres = getGenreChoices();
		Map<String, Genre> genreMap = new HashMap<>();
		for(Genre genre : validGenres)
			genreMap.put(genre.getIdentifier(), genre);
		
		FactoryManager.pullTransaction();
		try {
			for(int i=0; i<hiddenGenres.length; i++)
				if(genreMap.containsKey(hiddenGenres[i])) {
					genreMap.get(hiddenGenres[i]).setHidden(true);
					genreMap.remove(hiddenGenres[i]);
				}
			
			for(Genre genre : genreMap.values())
				genre.setHidden(false);
		} finally {
			FactoryManager.popTransaction();
		}
	}
	
	public String saveGenres() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		updateBlacklistedGenres();
		updateHiddenGenres();
		
		context.addMessage(null, new FacesMessage("Blacklisted genres have been updated"));
		
		return null;
	}
}
