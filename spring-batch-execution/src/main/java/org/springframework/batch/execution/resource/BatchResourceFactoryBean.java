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

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.StepExecution;
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
 * /%BATCH_ROOT%/job_data/%JOB_NAME%/%JOB_IDENTIFIER%-%STEP_NAME%.txt
 * </pre>
 * 
 * The %% variables are replaced with the corresponding bean property at run
 * time, when the factory method is executed. Note that the default pattern
 * starts with a forward slash "/", which means the root directory will be
 * interpreted as an absolute path if it too starts with "/" (because of the
 * implementation of the Spring Core Resource abstractions).<br/>
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
public class BatchResourceFactoryBean extends AbstractFactoryBean implements
		ResourceLoaderAware, StepContextAware {

	private static final String BATCH_ROOT_PATTERN = "%BATCH_ROOT%";

	private static final String JOB_NAME_PATTERN = "%JOB_NAME%";

	private static final String STEP_NAME_PATTERN = "%STEP_NAME%";

	private static final String DEFAULT_PATTERN = "/%BATCH_ROOT%/data/%JOB_NAME%/"
			+ "%STEP_NAME%.txt";

	private String filePattern = DEFAULT_PATTERN;

	private String jobName = null;

	private String rootDirectory = "";

	private String stepName = "";

	private ResourceLoader resourceLoader = new FileSystemResourceLoader();

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
		Assert.state(context.getStepExecution() != null,
				"The StepContext does not have an execution.");
		StepExecution execution = context.getStepExecution();
		stepName = execution.getStep().getName();
		jobName = execution.getStep().getJobInstance().getJobName();
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
	private String replacePattern(String string, String pattern,
			String replacement) {

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

		// TODO consider refactoring to void replacePattern() method and
		// collecting variable fileName
		fileName = replacePattern(fileName, BATCH_ROOT_PATTERN, rootDirectory);
		fileName = replacePattern(fileName, JOB_NAME_PATTERN,
				jobName == null ? "job" : jobName);
		fileName = replacePattern(fileName, STEP_NAME_PATTERN, stepName);

		return fileName;
	}

	public void setFilePattern(String filePattern) {
		this.filePattern = replacePattern(filePattern, "\\", File.separator);
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = replacePattern(rootDirectory, "\\", File.separator);
		if (rootDirectory != null && rootDirectory.endsWith(File.separator)) {
			this.rootDirectory = rootDirectory.substring(0, rootDirectory
					.lastIndexOf(File.separator));
		}
	}

}
