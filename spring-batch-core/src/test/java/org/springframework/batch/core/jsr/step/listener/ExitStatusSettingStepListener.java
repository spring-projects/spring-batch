package org.springframework.batch.core.jsr.step.listener;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.StepListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;

/**
 * <p>
 * {@link StepListener} for testing. Sets or appends the value of the
 * testProperty field to the {@link JobContext} exit status on afterStep.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class ExitStatusSettingStepListener implements StepListener {
	@Inject
	@BatchProperty
	private String testProperty;

	@Inject
	private JobContext jobContext;

	@Override
	public void beforeStep() throws Exception {

	}

	@Override
	public void afterStep() throws Exception {
		String exitStatus = jobContext.getExitStatus();

		if("".equals(exitStatus) || exitStatus == null) {
			jobContext.setExitStatus(testProperty);
		} else {
			jobContext.setExitStatus(exitStatus + testProperty);
		}
	}
}
