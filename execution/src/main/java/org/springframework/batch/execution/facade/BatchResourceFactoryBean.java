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

package org.springframework.batch.execution.facade;

import java.io.File;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * *******This class is currently undergoing heavy refactoring*****************
 * 
 * Strategy for locating different resources on the file system. For each unique
 * step, the same file handle will be returned. A unique step is defined as
 * having the same job name, job run, schedule date, stream name, and step name.
 * An external file mover (such as an EAI solution) should rename and move any
 * input files to conform to the patter defined by the file pattern.<br/>
 * 
 * If no pattern is passed in, then following default is used:
 * 
 * <pre>
 * %BATCH_ROOT%/job_data/%JOB_NAME%/%SCHEDULE_DATE%-%STREAM_NAME%-%STEP_NAME%.txt
 * </pre>
 * 
 * The %% variables are replaced with the corresponding bean property at run
 * time, when the factory method is executed.
 * 
 * @author Tomas Slanina
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see FactoryBean
 */
public class BatchResourceFactoryBean extends AbstractFactoryBean implements ResourceLoaderAware {

	private static final String BATCH_ROOT_PATTERN = "%BATCH_ROOT%";

	private static final String JOB_NAME_PATTERN = "%JOB_NAME%";

	private static final String JOB_RUN_PATTERN = "%JOB_RUN%";

	private static final String STEP_NAME_PATTERN = "%STEP_NAME%";

	private static final String STREAM_PATTERN = "%STREAM_NAME%";

	private static final String SCHEDULE_DATE_PATTERN = "%SCHEDULE_DATE%";

	private static final String DEFAULT_PATTERN = "%BATCH_ROOT%/job_data/%JOB_NAME%/"
			+ "%SCHEDULE_DATE%-%STREAM_NAME%-%STEP_NAME%.txt";

	private String filePattern = DEFAULT_PATTERN;

	private String jobName = "";

	private String jobStream = "";

	private int jobRun = 0;

	private String scheduleDate = "";

	private String rootDirectory = "";

	private String stepName = "";

	private ResourceLoader resourceLoader;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.ResourceLoaderAware#setResourceLoader(org.springframework.core.io.ResourceLoader)
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Returns the Resource representing the file defined by the file pattern.
	 * 
	 * @see FactoryBean#getObject()
	 * @return a resource representing the file on the file system.
	 */
	protected Object createInstance() {

		if (resourceLoader == null) {
			resourceLoader = new FileSystemResourceLoader();
		}

		return resourceLoader.getResource(createFileName());
	}

	public Class getObjectType() {
		return Resource.class;
	}

	/**
	 * helper method for <code>createFileName()</code>
	 */
	private String replacePattern(String string, String pattern, String replacement) {

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
		fileName = replacePattern(fileName, JOB_NAME_PATTERN, jobName);
		fileName = replacePattern(fileName, STEP_NAME_PATTERN, stepName);
		fileName = replacePattern(fileName, STREAM_PATTERN, jobStream);
		fileName = replacePattern(fileName, JOB_RUN_PATTERN, String.valueOf(jobRun));
		fileName = replacePattern(fileName, SCHEDULE_DATE_PATTERN, scheduleDate);

		return fileName;
	}

	public void setFilePattern(String filePattern) {
		this.filePattern = filePattern;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		if (rootDirectory!=null && rootDirectory.endsWith(File.separator)) {
			this.rootDirectory = rootDirectory.substring(0, rootDirectory.lastIndexOf(File.separator));
		}
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setJobRun(int jobRun) {
		this.jobRun = jobRun;
	}

	public void setJobStream(String jobStream) {
		this.jobStream = jobStream;
	}

	public void setScheduleDate(String scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

}
