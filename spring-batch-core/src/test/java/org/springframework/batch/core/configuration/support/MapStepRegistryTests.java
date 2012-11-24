package org.springframework.batch.core.configuration.support;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.tasklet.TaskletStep;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Sebastien Gerard
 */
public class MapStepRegistryTests {

    private static final String EXCEPTION_NOT_THROWN_MSG = "An exception should have been thrown";

    @Test
    public void registerStepEmptyCollection() {
        final StepRegistry stepRegistry = createRegistry();

        launchRegisterGetRegistered(stepRegistry, "myJob", getStepCollection());
    }

    @Test
    public void registerStepNullJobName() {
        final StepRegistry stepRegistry = createRegistry();

        try {
            stepRegistry.register(null, new HashSet<Step>());
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void registerStepNullSteps() {
        final StepRegistry stepRegistry = createRegistry();

        try {
            stepRegistry.register("fdsfsd", null);
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void registerStepGetStep() {
        final StepRegistry stepRegistry = createRegistry();

        launchRegisterGetRegistered(stepRegistry, "myJob",
                getStepCollection(
                        createStep("myStep"),
                        createStep("myOtherStep"),
                        createStep("myThirdStep")
                ));
    }

    @Test
    public void getJobNotRegistered() {
        final StepRegistry stepRegistry = createRegistry();

        final String aStepName = "myStep";
        launchRegisterGetRegistered(stepRegistry, "myJob",
                getStepCollection(
                        createStep(aStepName),
                        createStep("myOtherStep"),
                        createStep("myThirdStep")
                ));

        assertJobNotRegistered(stepRegistry, "a ghost");
    }

    @Test
    public void getJobNotRegisteredNoRegistration() {
        final StepRegistry stepRegistry = createRegistry();

        assertJobNotRegistered(stepRegistry, "a ghost");
    }

    @Test
    public void getStepNotRegistered() {
        final StepRegistry stepRegistry = createRegistry();

        final String jobName = "myJob";
        launchRegisterGetRegistered(stepRegistry, jobName,
                getStepCollection(
                        createStep("myStep"),
                        createStep("myOtherStep"),
                        createStep("myThirdStep")
                ));

        assertStepNameNotRegistered(stepRegistry, jobName, "fsdfsdfsdfsd");
    }

    @Test
    public void registerRegisterAgainAndGet() {
        final StepRegistry stepRegistry = createRegistry();

        final String jobName = "myJob";
        final Collection<Step> stepsFirstRegistration = getStepCollection(
                createStep("myStep"),
                createStep("myOtherStep"),
                createStep("myThirdStep")
        );

        // first registration
        launchRegisterGetRegistered(stepRegistry, jobName, stepsFirstRegistration);

        // register again the job
        launchRegisterGetRegistered(stepRegistry, jobName,
                getStepCollection(
                        createStep("myFourthStep"),
                        createStep("lastOne")
                ));

        assertStepsNotRegistered(stepRegistry, jobName, stepsFirstRegistration);
    }

    @Test
    public void getStepNullJobName() throws NoSuchJobException {
        final StepRegistry stepRegistry = createRegistry();

        try {
            stepRegistry.getStep(null, "a step");
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getStepNullStepName() throws NoSuchJobException {
        final StepRegistry stepRegistry = createRegistry();

        final String stepName = "myStep";
        launchRegisterGetRegistered(stepRegistry, "myJob", getStepCollection(createStep(stepName)));

        try {
            stepRegistry.getStep(null, stepName);
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void registerStepUnregisterJob() {
        final StepRegistry stepRegistry = createRegistry();

        final Collection<Step> steps = getStepCollection(
                createStep("myStep"),
                createStep("myOtherStep"),
                createStep("myThirdStep")
        );

        final String jobName = "myJob";
        launchRegisterGetRegistered(stepRegistry, jobName, steps);

        stepRegistry.unregisterStepsFromJob(jobName);
        assertJobNotRegistered(stepRegistry, jobName);
    }

    @Test
    public void unregisterJobNameNull() {
        final StepRegistry stepRegistry = createRegistry();

        try {
            stepRegistry.unregisterStepsFromJob(null);
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void unregisterNoRegistration() {
        final StepRegistry stepRegistry = createRegistry();

        assertJobNotRegistered(stepRegistry, "a job");
    }

    protected StepRegistry createRegistry() {
        return new MapStepRegistry();
    }

    protected Step createStep(String stepName) {
        return new TaskletStep(stepName);
    }

    protected Collection<Step> getStepCollection(Step... steps) {
        return Arrays.asList(steps);
    }

    protected void launchRegisterGetRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps) {
        stepRegistry.register(jobName, steps);
        assertStepsRegistered(stepRegistry, jobName, steps);
    }

    protected void assertJobNotRegistered(StepRegistry stepRegistry, String jobName) {
        try {
            stepRegistry.getStep(jobName, "a step");
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (NoSuchJobException e) {
        }
    }

    protected void assertStepsRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps) {
        for (Step step : steps) {
            try {
                stepRegistry.getStep(jobName, step.getName());
            } catch (NoSuchJobException e) {
                Assert.fail("Unexpected exception " + e);
            }
        }
    }

    protected void assertStepsNotRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps) {
        for (Step step : steps) {
            assertStepNameNotRegistered(stepRegistry, jobName, step.getName());
        }
    }

    protected void assertStepNameNotRegistered(StepRegistry stepRegistry, String jobName, String stepName) {
        try {
            stepRegistry.getStep(jobName, stepName);
            Assert.fail(EXCEPTION_NOT_THROWN_MSG);
        } catch (NoSuchJobException e) {
            Assert.fail("Unexpected exception");
        } catch (NoSuchStepException e) {
        }
    }
}