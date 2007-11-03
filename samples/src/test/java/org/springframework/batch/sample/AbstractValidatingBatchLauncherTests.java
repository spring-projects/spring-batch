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

import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Abstract TestCase that automatically starts a Spring (@link Lifecycle) after
 * obtaining it automatically via autowiring by type. It should be noted the
 * getConfigLocations must be implemented for dependency injection to work
 * properly.
 * 
 * @author Lucas Ward
 * @see AbstractDependencyInjectionSpringContextTests
 */
public abstract class AbstractValidatingBatchLauncherTests extends AbstractBatchLauncherTests {

	public void testLaunchJob() throws Exception {
		validatePreConditions();
		launcher.run(getJobName());
		launcher.stop();
		validatePostConditions();
	}

	/**
	 * Make sure input data meets expectations
	 */
	protected void validatePreConditions() throws Exception {
	}

	/**
	 * Make sure job did what it was expected to do.
	 */
	protected abstract void validatePostConditions() throws Exception;

}
