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

package org.springframework.batch.execution.resource;

import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.execution.bootstrap.support.DefaultJobParametersFactory;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepContextAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Strategy for locating different resources on the file system. For each unique
 * step, the same file handle will be returned. A unique step is defined as
 * having the same job identifier and step name. An external file mover (such as
 * an EAI solution) should rename and move any input files to conform to the
 * patter defined by the file pattern.<br/>
 * 
 * If no pattern is passed in, then following default is used:
 * 
 * <pre>
 * data/%JOB_NAME%/%STEP_NAME%.txt
 * </pre>
 * 
 * The %% variables are replaced with the corresponding bean property at run
 * time, when the factory method is executed. To insert {@link JobParameters}
 * use a pattern with the parameter key surrounded by %%, e.g.
 * 
 * <pre>
 * //home/jobs/data/%JOB_NAME%/%STEP_NAME%-%schedule.date%.txt
 * </pre>
 * 
 * Note that the default pattern does not start with a separator. Because of the
 * implementation of the Spring Core Resource abstractions, it would need to
 * start with a double forward slash "//" to resolve to an absolute directory.<br/>
 * 
 * It doesn't make much sense to use this factory unless it is step scoped, but
 * note that it is thread safe only if it is step scoped and its mutators are
 * not used except for configuration.
 * 
 * @author Tomas Slanina
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see FactoryBean
 */
public class BatchResourceFactoryBean extends AbstractFactoryBean implements ResourceLoaderAware, StepContextAware {

	private static final String JOB_NAME_PATTERN = "%JOB_NAME%";

	private static final String STEP_NAME_PATTERN = "%STEP_NAME%";

	private static final String DEFAULT_PATTERN = "data/%JOB_NAME%/" + "%STEP_NAME%.txt";

	private String filePattern = DEFAULT_PATTERN;

	private String jobName = null;

	private String stepName = "";

	private JobParametersFactory jobParametersFactory = new DefaultJobParametersFactory();

	private ResourceLoader resourceLoader = new FileSystemResourceLoader();

	private Properties properties;

	/**
	 * Public setter for the {@link JobParametersFactory} used to translate
	 * {@link JobParameters} into {@link Properties}. Defaults to a
	 * {@link DefaultJobParametersFactory}.
	 * @param jobParametersFactory the {@link JobParametersFactory} to set
	 */
	public void setJobParametersFactory(JobParametersFactory jobParametersFactory) {
		this.jobParametersFactory = jobParametersFactory;
	}

	/**
	 * Always false because we are expecting to be step scoped.
	 * 
	 * @see org.springframework.beans.factory.config.AbstractFactoryBean#isSingleton()
	 */
	public boolean isSingleton() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Collect the properties of the enclosing {@link StepExecution} that will
	 * be needed to create a file name.
	 * 
	 * @see org.springframework.batch.execution.scope.StepContextAware#setStepScopeContext(org.springframework.core.AttributeAccessor)
	 */
	public void setStepContext(StepContext context) {
		Assert.state(context.getStepExecution() != null, "The StepContext does not have an execution.");
		StepExecution execution = context.getStepExecution();
		stepName = execution.getStep().getName();
		jobName = execution.getStep().getJobInstance().getJobName();
		properties = jobParametersFactory.getProperties(execution.getStep().getJobInstance().getJobParameters());
	}

	/**
	 * Returns the Resource representing the file defined by the file pattern.
	 * 
	 * @see FactoryBean#getObject()
	 * @return a resource representing the file on the file system.
	 */
	protected Object createInstance() {
		return resourceLoader.getResource(createFileName());
	}

	public Class getObjectType() {
		return Resource.class;
	}

	/**
	 * helper method for <code>createFileName()</code>
	 */
	private String replacePattern(String string, String pattern, String replacement) {

		if (string == null)
			return null;

		// check to ensure pattern exists in string.
		if (string.indexOf(pattern) != -1) {
			return StringUtils.replace(string, pattern, replacement);
		}

		return string;
	}

	/**
	 * Creates a filename given a pattern and step context information.
	 * 
	 * Deliberate package access, so that the method can be accessed by unit
	 * tests
	 */
	private String createFileName() {
		Assert.notNull(filePattern, "filename pattern is null");

		String fileName = filePattern;

		fileName = replacePattern(fileName, JOB_NAME_PATTERN, jobName == null ? "job" : jobName);
		fileName = replacePattern(fileName, STEP_NAME_PATTERN, stepName);

		if (properties != null) {
			for (Iterator iterator = properties.entrySet().iterator(); iterator.hasNext();) {
				Entry entry = (Entry) iterator.next();
				String key = (String) entry.getKey();
				fileName = replacePattern(fileName, "%" + key + "%", (String) entry.getValue());
			}
		}

		return fileName;
	}

	public void setFilePattern(String filePattern) {
		this.filePattern = replacePattern(filePattern, "\\", File.separator);
	}

}
