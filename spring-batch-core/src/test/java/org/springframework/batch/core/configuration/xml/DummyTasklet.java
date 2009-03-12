package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class DummyTasklet implements Tasklet {

	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		return null;
	}

}
