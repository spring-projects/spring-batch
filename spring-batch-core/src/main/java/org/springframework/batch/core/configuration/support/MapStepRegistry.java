package org.springframework.batch.core.configuration.support;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple map-based implementation of {@link StepRegistry}. Access to the map is
 * synchronized, guarded by an internal lock.
 *
 * @author Sebastien Gerard
 * @author Stephane Nicoll
 */
public class MapStepRegistry implements StepRegistry {

    private final Map<String, Map<String, Step>> map = new HashMap<String, Map<String, Step>>();

    public void register(String jobName, Collection<Step> steps) {
        Assert.notNull(jobName, "The job name cannot be null.");
        Assert.notNull(steps, "The job steps cannot be null.");

        unregisterStepsFromJob(jobName);

        synchronized (this.map) {
            final Map<String, Step> jobSteps = new HashMap<String, Step>();
            for (Step step : steps) {
                jobSteps.put(step.getName(), step);
            }

            this.map.put(jobName, jobSteps);
        }
    }

    public void unregisterStepsFromJob(String jobName) {
        Assert.notNull(jobName, "Job configuration must have a name.");
        synchronized (map) {
            map.remove(jobName);
        }
    }

    public Step getStep(String jobName, String stepName) throws NoSuchJobException {
        Assert.notNull(jobName, "The job name cannot be null.");
        Assert.notNull(stepName, "The step name cannot be null.");

        synchronized (map) {
            if (!map.containsKey(jobName)) {
                throw new NoSuchJobException("No job configuration with the name [" + jobName + "] was registered");
            } else {
                final Map<String, Step> jobSteps = map.get(jobName);
                if (jobSteps.containsKey(stepName)) {
                    return jobSteps.get(stepName);
                } else {
                    throw new NoSuchStepException("The step called [" + stepName + "] does not exist in the job [" +
                            jobName + "]");
                }
            }
        }
    }

}
