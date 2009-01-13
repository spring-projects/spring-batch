/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Base class for testing batch jobs using the SimpleJob implementation. 
 * It provides methods for launching a Job, or individual Steps within a Job on their own,
 * allowing for end to end testing of individual steps, without having to run every step
 * in the job.  Any test classes inheriting from this class should make sure they are part
 * of an ApplicationContext, which is generally expected to be done as part of the Spring
 * test framework.  Furthermore, the ApplicationContext in which it is a part of is expected
 * to have one {@link JobLauncher}, {@link JobRepository}, and a single Job implementation.
 * It should be noted that using any of the methods that don't conain {@link JobParameters} 
 * in their signature, will result in one being created with the current system time as a
 * parameter.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 */
public abstract class AbstractSimpleJobTests extends AbstractJobTests {

	private StepRunner stepRunner;

	private Map<String, Step> stepMap = new HashMap<String, Step>();
	private List<Step> stepList = new ArrayList<Step>();

	@Before
	public void setUpSteps() {
		for (Step step : (getSimpleJob()).getSteps()) {
			stepMap.put(step.getName(), step);
			stepList.add(step);
		}
	}

	protected StepRunner getStepRunner() {
		if(stepRunner == null){
			stepRunner = new StepRunner(getJobLauncher(), getJobRepository());
		}
		return stepRunner;
	}
	
	public SimpleJob getSimpleJob() {
		return (SimpleJob)getJob();
	}

	public Step getStep(String stepName){
		
		if(!stepMap.containsKey(stepName)){
			throw new IllegalStateException("No Step found with name: [" + stepName + "]");
		}
		return stepMap.get(stepName);
	}

	/**
	 * Launch just the specified step in the job.
	 * 
	 * @param stepName
	 */
	public JobExecution launchStep(String stepName) {
		return getStepRunner().launchStep(getStep(stepName));
	}

	/**
	 * Launch just the specified step in the job.
	 * 
	 * @param stepName
	 * @param jobParameters
	 */
	public JobExecution launchStep(String stepName, JobParameters jobParameters) {
		return getStepRunner().launchStep(getStep(stepName), jobParameters);
	}

}
