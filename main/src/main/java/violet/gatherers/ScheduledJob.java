package violet.gatherers;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import violet.jpa.FactoryManager;

/**
 * Runs all gatherers to update all games
 * @author somer
 */
@DisallowConcurrentExecution
public class ScheduledJob implements InterruptableJob {
	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap jobData = context.getJobDetail().getJobDataMap();
		
		boolean insertOnly = jobData.getBoolean("insert-only");
		int maxGames = jobData.getInt("max-games");
		
		Stack<Gatherer> gatherers = new Stack<Gatherer>();
		//gatherers.push(new SteamGatherer());
		gatherers.push(new PlaystationGatherer());
		//gatherers.push(new XBoxGatherer());
		
		processGatherers(insertOnly, maxGames, gatherers);
	}
	
	private Gatherer currentGatherer = null;
	
	public void processGatherers(boolean insertOnly, int maxGames, Stack<Gatherer> gatherers) {
		while(!gatherers.isEmpty()) { // loop through all our gatherers
			currentGatherer = gatherers.pop();
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Executing " + currentGatherer.getClass().getName());
			FactoryManager.pullCommonEM();
			try { // run gather on all gatherers
				currentGatherer.gather(insertOnly, maxGames);
			} catch(Exception e) {
				
				Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Failed executing", e);
			} finally {
				FactoryManager.popCommonEM();
			}
			Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Done");
		}
	}

	public void interrupt() throws UnableToInterruptJobException {
		if(currentGatherer != null) // interrupt the currently running gatherer
			currentGatherer.interrupt();
		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Interrupted job");
	}
}
