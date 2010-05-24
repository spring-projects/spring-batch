package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

public class NoopTasklet extends NameStoringTasklet {

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        super.execute(contribution, chunkContext);
        contribution.setExitStatus(ExitStatus.NOOP);        
        return RepeatStatus.FINISHED;
    }
}
