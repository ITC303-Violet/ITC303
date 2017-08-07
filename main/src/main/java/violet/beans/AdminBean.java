package violet.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import violet.jpa.FactoryManager;
import violet.jpa.Game;
import violet.jpa.Genre;

/**
 * Handles administration requests such as triggering an update of the games list
 * @author somer
 */
@ManagedBean
@RequestScoped
public class AdminBean extends JPABean.JPAEquippedBean {
	private int maxGamesGather;
	private Long gamesCount;
	
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
	
	public Long getGamesCount() {
		if(gamesCount == null)
			gamesCount = Game.count();
		
		return gamesCount;
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
	
	
	public List<Genre> getBlacklistedGenreChoices() {
		return new ArrayList<Genre>(Genre.getGenres(FactoryManager.getCommonEM(), true));
	}
	
	String[] selectedGenres;
	
	public String[] getBlacklistedGenres() {
		EntityManager em = FactoryManager.getCommonEM();
		if(selectedGenres == null) {
			List<Genre> genres = em.createQuery("SELECT g FROM Genre g WHERE g.blacklisted=TRUE", Genre.class).getResultList();
			selectedGenres = new String[genres.size()];
			for(int i=0; i<genres.size(); i++)
				selectedGenres[i] = genres.get(i).getIdentifier();
		}
		
		return selectedGenres;
	}
	
	public void setBlacklistedGenres(String[] genres) {
		selectedGenres = genres;
	}
	
	private void updateBlacklistedGenres() {
		List<Genre> validGenres = getBlacklistedGenreChoices();
		Map<String, Genre> genreMap = new HashMap<>();
		for(Genre genre : validGenres)
			genreMap.put(genre.getIdentifier(), genre);
		
		FactoryManager.pullTransaction();
		try {
			for(int i=0; i<selectedGenres.length; i++)
				if(genreMap.containsKey(selectedGenres[i])) {
					genreMap.get(selectedGenres[i]).setBlacklisted(true);
					genreMap.remove(selectedGenres[i]);
				}
			
			for(Genre genre : genreMap.values())
				genre.setBlacklisted(false);
		} finally {
			FactoryManager.popTransaction();
		}
	}
	
	public String saveGenres() {
		FacesContext context = FacesContext.getCurrentInstance();
		
		updateBlacklistedGenres();
		
		context.addMessage(null, new FacesMessage("Blacklisted genres have been updated"));
		
		return null;
	}
}
