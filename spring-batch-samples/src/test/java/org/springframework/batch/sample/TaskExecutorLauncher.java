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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class TaskExecutorLauncher {

	public static void main(String[] args) throws Exception {

		// Paths to individual job configurations. Each one must include the
		// step scope and the jobConfigurationRegistryBeanPostProcessor.
		final String[] paths = new String[] { "jobs/adhocLoopJob.xml",
				"jobs/footballJob.xml" };

		// The simple execution environment will be used as a parent
		// context for each of the job contexts. The standard version of this
		// from the Spring Batch samples does not have an MBean for the
		// JobLauncher, nor does the JobLauncher have an asynchronous
		// TaskExecutor. The adhocLoopJob has both, which is why it has to be
		// included in the paths above.
		final ApplicationContext parent = new ClassPathXmlApplicationContext(
				"simple-container-definition.xml");

		new Thread(new Runnable() {
			public void run() {
				for (int i = 0; i < paths.length; i++) {
					String path = paths[i];
					new ClassPathXmlApplicationContext(new String[] { path },
							parent);
				}
			};
		}).start();

		System.out
				.println("Started application.  "
						+ "Please connect using JMX (remember to use -Dcom.sun.management.jmxremote if you can't see anything in Jconsole).");
		System.in.read();

	}
}
