package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class TestTasklet extends AbstractTestComponent implements Tasklet {

	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		executed = true;
		contribution.setExitStatus(ExitStatus.FINISHED);
		return RepeatStatus.FINISHED;  
	}  

}
