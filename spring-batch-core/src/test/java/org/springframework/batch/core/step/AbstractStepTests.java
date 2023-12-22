package org.springframework.batch.core.step;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for {@link AbstractStep}.
 */
public class AbstractStepTests {

    AbstractStep tested = new AbstractStep() {
        @Override
        protected void doExecute(StepExecution stepExecution) {
        }
    };

    StepExecution execution = new StepExecution("foo",
            new JobExecution(new JobInstance(1L, "bar"), new JobParameters()));

    JobRepository jobRepository = new JobRepositorySupport();


    @Test
    void testSetEndTime() throws Exception {
        final List<LocalDateTime> stepEndTime = new ArrayList<>();

        tested.setStepExecutionListeners(new StepExecutionListener[] { new StepExecutionListener() {
            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                stepEndTime.add(stepExecution.getEndTime());
                return ExitStatus.COMPLETED;
            }
        } });

        tested.setJobRepository(jobRepository);
        tested.execute(execution);

        assertEquals(1, stepEndTime.size());
        assertNotNull(stepEndTime.get(0));
    }
}
