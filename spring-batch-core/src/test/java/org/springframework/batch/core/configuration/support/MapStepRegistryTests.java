/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.configuration.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.configuration.StepRegistry;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.tasklet.TaskletStep;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Gerard
 */
class MapStepRegistryTests {

	@Test
	void registerStepEmptyCollection() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		launchRegisterGetRegistered(stepRegistry, "myJob", getStepCollection());
	}

	@Test
	void registerStepNullJobName() {
		StepRegistry stepRegistry = createRegistry();
		assertThrows(IllegalArgumentException.class, () -> stepRegistry.register(null, new HashSet<>()));
	}

	@Test
	void registerStepNullSteps() {
		StepRegistry stepRegistry = createRegistry();
		assertThrows(IllegalArgumentException.class, () -> stepRegistry.register("fdsfsd", null));
	}

	@Test
	void registerStepGetStep() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		launchRegisterGetRegistered(stepRegistry, "myJob",
				getStepCollection(createStep("myStep"), createStep("myOtherStep"), createStep("myThirdStep")));
	}

	@Test
	void getJobNotRegistered() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		final String aStepName = "myStep";
		launchRegisterGetRegistered(stepRegistry, "myJob",
				getStepCollection(createStep(aStepName), createStep("myOtherStep"), createStep("myThirdStep")));

		assertJobNotRegistered(stepRegistry, "a ghost");
	}

	@Test
	void getJobNotRegisteredNoRegistration() {
		final StepRegistry stepRegistry = createRegistry();

		assertJobNotRegistered(stepRegistry, "a ghost");
	}

	@Test
	void getStepNotRegistered() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		final String jobName = "myJob";
		launchRegisterGetRegistered(stepRegistry, jobName,
				getStepCollection(createStep("myStep"), createStep("myOtherStep"), createStep("myThirdStep")));

		assertStepNameNotRegistered(stepRegistry, jobName, "fsdfsdfsdfsd");
	}

	@Test
	void registerTwice() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		final String jobName = "myJob";
		final Collection<Step> stepsFirstRegistration = getStepCollection(createStep("myStep"),
				createStep("myOtherStep"), createStep("myThirdStep"));

		// first registration
		launchRegisterGetRegistered(stepRegistry, jobName, stepsFirstRegistration);

		// Second registration with same name should fail
		assertThrows(DuplicateJobException.class, () -> stepRegistry.register(jobName,
				getStepCollection(createStep("myFourthStep"), createStep("lastOne"))));
	}

	@Test
	void getStepNullJobName() {
		StepRegistry stepRegistry = createRegistry();
		assertThrows(IllegalArgumentException.class, () -> stepRegistry.getStep(null, "a step"));
	}

	@Test
	void getStepNullStepName() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		final String stepName = "myStep";
		launchRegisterGetRegistered(stepRegistry, "myJob", getStepCollection(createStep(stepName)));
		assertThrows(IllegalArgumentException.class, () -> stepRegistry.getStep(null, stepName));
	}

	@Test
	void registerStepUnregisterJob() throws DuplicateJobException {
		final StepRegistry stepRegistry = createRegistry();

		final Collection<Step> steps = getStepCollection(createStep("myStep"), createStep("myOtherStep"),
				createStep("myThirdStep"));

		final String jobName = "myJob";
		launchRegisterGetRegistered(stepRegistry, jobName, steps);

		stepRegistry.unregisterStepsFromJob(jobName);
		assertJobNotRegistered(stepRegistry, jobName);
	}

	@Test
	void unregisterJobNameNull() {
		StepRegistry stepRegistry = createRegistry();
		assertThrows(IllegalArgumentException.class, () -> stepRegistry.unregisterStepsFromJob(null));
	}

	@Test
	void unregisterNoRegistration() {
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

	protected void launchRegisterGetRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps)
			throws DuplicateJobException {
		stepRegistry.register(jobName, steps);
		assertStepsRegistered(stepRegistry, jobName, steps);
	}

	protected void assertJobNotRegistered(StepRegistry stepRegistry, String jobName) {
		assertNull(stepRegistry.getStep(jobName, "a step"));
	}

	protected void assertStepsRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps) {
		for (Step step : steps) {
			assertDoesNotThrow(() -> stepRegistry.getStep(jobName, step.getName()));
		}
	}

	protected void assertStepsNotRegistered(StepRegistry stepRegistry, String jobName, Collection<Step> steps) {
		for (Step step : steps) {
			assertStepNameNotRegistered(stepRegistry, jobName, step.getName());
		}
	}

	protected void assertStepNameNotRegistered(StepRegistry stepRegistry, String jobName, String stepName) {
		assertNull(stepRegistry.getStep(jobName, stepName));
	}

}