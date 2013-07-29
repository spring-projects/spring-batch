/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.listener.JobListener;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.xml.JobParserJobFactoryBean;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.jsr.JobListenerAdapter;
import org.springframework.beans.factory.FactoryBean;

/**
 * This {@link FactoryBean} is used by the JSR-352 namespace parser to create
 * {@link FlowJob} objects. It stores all of the properties that are
 * configurable on the &lt;job/&gt;.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JobFactoryBean extends JobParserJobFactoryBean {

	public JobFactoryBean(String name) {
		super(name);
	}

	/**
	 * Addresses wrapping {@link JobListener} as needed to be used with
	 * the framework.
	 *
	 * @param jobListeners a list of all job listeners
	 */
	public void setJobExecutionListeners(Object[] jobListeners) {
		if(jobListeners != null) {
			JobExecutionListener[] listeners = new JobExecutionListener[jobListeners.length];

			for(int i = 0; i < jobListeners.length; i++) {
				Object curListener = jobListeners[i];
				if(curListener instanceof JobExecutionListener) {
					listeners[i] = (JobExecutionListener) curListener;
				} else if(curListener instanceof JobListener){
					listeners[i] = new JobListenerAdapter((JobListener) curListener);
				}
			}

			super.setJobExecutionListeners(listeners);
		}
	}
}
