package org.springframework.batch.core.test.timeout;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class SleepingTasklet implements Tasklet {
	
	private long millisToSleep;

	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		Thread.sleep(millisToSleep);
		return RepeatStatus.FINISHED;
	}
	
	public void setMillisToSleep(long millisToSleep) {
		this.millisToSleep = millisToSleep;
	}

}
