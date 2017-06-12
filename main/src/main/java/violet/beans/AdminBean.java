package violet.beans;

import java.io.IOException;

import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import violet.gatherers.ScheduledJob;

/**
 * Handles administration requests such as triggering an update of the games list
 * @author somer
 */
@ManagedBean
@RequestScoped
public class AdminBean {
	private int maxGames;
	
	@ManagedProperty(value = "#{userBean}")
	private UserBean userBean;
	
	@ManagedProperty(value = "#{quartzBean}")
	private QuartzBean quartzBean;
	
	public int getMaxGames() {
		return maxGames;
	}
	
	public void setMaxGames(int maxGames) {
		this.maxGames = maxGames;
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
				.usingJobData("max-games", maxGames)
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
}
