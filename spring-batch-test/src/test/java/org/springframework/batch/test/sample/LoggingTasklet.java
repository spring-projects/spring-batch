package org.springframework.batch.test.sample;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class LoggingTasklet implements Tasklet {

        protected static final Log logger = LogFactory.getLog(LoggingTasklet.class);

        private int id = 0;

        public LoggingTasklet(int id) {
                this.id = id;
        }

        public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                logger.info("tasklet executing: id=" + id);
                return RepeatStatus.FINISHED;
        }
}
