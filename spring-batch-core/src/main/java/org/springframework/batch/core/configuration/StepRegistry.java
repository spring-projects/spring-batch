package org.springframework.batch.core.configuration;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;

import java.util.Collection;

/**
 * Registry keeping track of all the {@link Step} defined in a
 * {@link org.springframework.batch.core.Job}.
 *
 * @author Sebastien Gerard
 * @author Stephane Nicoll
 */
public interface StepRegistry {

    /**
     * Registers all the step of the given job. If the job is already registered,
     * the method {@link #unregisterStepsFromJob(String)} is called before registering
     * the given steps.
     *
     * @param jobName the give job name
     * @param steps   the job steps
     */
    void register(String jobName, Collection<Step> steps);

    /**
     * Unregisters all the steps of the given job. If the job is not registered,
     * nothing happens.
     *
     * @param jobName the given job name
     */
    void unregisterStepsFromJob(String jobName);

    /**
     * Returns the {@link Step} of the specified job based on its name.
     *
     * @param jobName  the name of the job
     * @param stepName the name of the step to retrieve
     * @return the step with the given name belonging to the mentioned job
     * @throws NoSuchJobException  no such job with that name exists
     * @throws NoSuchStepException no such step with that name for that job exists
     */
    Step getStep(String jobName, String stepName) throws NoSuchJobException, NoSuchStepException;

}
