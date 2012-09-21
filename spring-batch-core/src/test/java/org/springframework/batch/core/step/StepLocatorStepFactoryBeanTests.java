package org.springframework.batch.core.step;

import org.junit.Test;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.SimpleJob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for StepLocatorStepFactoryBean.
 * 
 * @author tvaughan
 */
public class StepLocatorStepFactoryBeanTests {
    
    @Test
    public void testFoo() throws Exception {
        Step testStep1 = buildTestStep("foo");
        Step testStep2 = buildTestStep("bar");
        Step testStep3 = buildTestStep("baz");
        
        SimpleJob simpleJob = new SimpleJob();   // is a StepLocator
        simpleJob.addStep(testStep1);
        simpleJob.addStep(testStep2);
        simpleJob.addStep(testStep3);
        
        StepLocatorStepFactoryBean stepLocatorStepFactoryBean = new StepLocatorStepFactoryBean();
        stepLocatorStepFactoryBean.setStepLocator(simpleJob);
        stepLocatorStepFactoryBean.setStepName("bar");
        assertEquals(testStep2, stepLocatorStepFactoryBean.getObject());
    }
    
    private Step buildTestStep(final String stepName) {
        return new Step() {
            public String getName() {
                return stepName;
            }

            public boolean isAllowStartIfComplete() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public int getStartLimit() {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void execute(StepExecution stepExecution) throws JobInterruptedException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
    
    @Test
    public void testGetObjectType() {
        assertTrue((new StepLocatorStepFactoryBean()).getObjectType().isAssignableFrom(Step.class));
    }
}
