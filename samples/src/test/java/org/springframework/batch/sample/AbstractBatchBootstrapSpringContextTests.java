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

package org.springframework.batch.sample;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.execution.bootstrap.AbstractJobLauncher;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Abstract unit test for running functional tests by getting context locations for
 * both the container and configuration separately and having them auto wired in 
 * by type.  This allows the two to be completely separated, and remove any
 * 'configuration coupling' between the two.  However, it is still purely
 * theoretical until a decision is made as to how job configuration and container 
 * configuration files are pulled together.
 * 
 * @author Lucas Ward
 *
 */
public abstract class AbstractBatchBootstrapSpringContextTests extends AbstractDependencyInjectionSpringContextTests {

	private static final String CONTAINER_DEFINITION_LOCATION = "simple-container-definition.xml";
	
	AbstractJobLauncher bootstrap;
	JobConfiguration jobConfiguration;
	
	protected String[] getConfigLocations() {
		return new String[]{CONTAINER_DEFINITION_LOCATION, getJobConfigurationContextLocation()};
	}
	
	public void testLifecycle(){
		bootstrap.start();
	}
	
	public void setBootstrap(AbstractJobLauncher bootstrap){
		this.bootstrap = bootstrap;
	}
	
	protected abstract String getJobConfigurationContextLocation();
}
