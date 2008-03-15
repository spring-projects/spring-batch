/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.core.step;

import org.springframework.core.JdkVersion;
import org.springframework.util.ClassUtils;

/**
 * A factory that properly determines which version of the {@link StepExecutionSynchronizer} to return based on the
 * availabilty of Java 5 or Backport Concurrent.
 * 
 * @author Ben Hale
 */
public class StepExecutionSyncronizerFactory {

	/** Whether the backport-concurrent library is present on the classpath */
	private static final boolean backportConcurrentAvailable = ClassUtils.isPresent(
	        "edu.emory.mathcs.backport.java.util.concurrent.Semaphore", StepExecutionSyncronizerFactory.class
	                .getClassLoader());

	private final StepExecutionSynchronizer synchronizer;

	public StepExecutionSyncronizerFactory() {
		if (JdkVersion.isAtLeastJava15()) {
			synchronizer = new JdkConcurrentStepExecutionSynchronizer();
		} else if (backportConcurrentAvailable) {
			synchronizer = new BackportConcurrentStepExecutionSynchronizer();
		} else {
			throw new IllegalStateException("Cannot create StepExecutionSycnronizer - "
			        + "neither JDK 1.5 nor backport-concurrent available on the classpath");
		}
	}

	public StepExecutionSynchronizer getStepExecutionSynchronizer() {
		return synchronizer;
	}
}
