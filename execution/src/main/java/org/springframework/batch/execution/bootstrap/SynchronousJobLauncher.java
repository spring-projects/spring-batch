package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.repeat.ExitStatus;

public interface SynchronousJobLauncher {

	boolean isRunning();

	ExitStatus run();

	ExitStatus run(String jobName) throws NoSuchJobConfigurationException;

	void stop();

}
