package org.springframework.batch.execution.bootstrap.support;

import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.execution.launch.JobLauncher;

/**
 * Mock Job Launcher.  Normally, something like EasyMock would
 * be used to mock an interface, however, because of the nature
 * of launching a batch job from the command line, the mocked
 * class cannot be injected.
 *
 * @author Lucas Ward
 *
 */
public class StubJobLauncher implements JobLauncher {

	public static final int RUN_NO_ARGS = 0;
	public static final int RUN_JOB_NAME = 1;
	public static final int RUN_JOB_IDENTIFIER =2 ;

	private int lastRunCalled = RUN_NO_ARGS;
	private JobExecution returnValue = null;

	private boolean isRunning = false;

	public boolean isRunning() {
		return isRunning;
	}

	public JobExecution run(JobIdentifier jobIdentifier)
			throws NoSuchJobException {
		lastRunCalled = RUN_JOB_IDENTIFIER;
		return returnValue;
	}

	public void stop() {

	}

	public void setReturnValue(JobExecution returnValue){
		this.returnValue = returnValue;
	}

	public void setIsRunning(boolean isRunning){
		this.isRunning = isRunning;
	}

	public int getLastRunCalled(){
		return lastRunCalled;
	}
}
