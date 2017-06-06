package violet.beans;

import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.ee.servlet.QuartzInitializerListener;
import org.quartz.impl.StdSchedulerFactory;

@ManagedBean
@ApplicationScoped
public class QuartzBean {
	private Scheduler scheduler;
	
	public StdSchedulerFactory getSchedulerFactory() {
		ServletContext servletContext = (ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext();
		return (StdSchedulerFactory)servletContext
				.getAttribute(QuartzInitializerListener.QUARTZ_FACTORY_KEY);
	}
	
	public Scheduler getScheduler() {
		if(scheduler == null) {
			try {
				scheduler = getSchedulerFactory().getScheduler();
			} catch(SchedulerException e) {
				e.printStackTrace();
			}
		}
		return scheduler;
	}
}
