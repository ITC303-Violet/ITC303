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
	
	public String gather() {
		if(!userBean.isStaff())
			return null;
		
		FacesContext context = FacesContext.getCurrentInstance();
		
		JobDetail job = JobBuilder.newJob(ScheduledJob.class)
				.withIdentity("manual-gather", "GATHERERS")
				.usingJobData("insert-only", false)
				.usingJobData("max-games", maxGames)
				.build();
		
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
			externalContext.redirect(request.getContextPath());
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}
