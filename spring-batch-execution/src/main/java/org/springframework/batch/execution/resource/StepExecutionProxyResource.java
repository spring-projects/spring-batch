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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.core.listener.StepListenerSupport;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.execution.bootstrap.DefaultJobParametersFactory;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Strategy for locating different resources on the file system. For each unique
 * step execution, the same file handle will be returned. A unique step is
 * defined as having the same job instance and step name. An external file mover
 * (such as an EAI solution) should rename and move any input files to conform
 * to the pattern defined here.<br/>
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
 * To use this resource it must be initialised with a {@link StepExecution}.
 * The best way to do that is to register it as a listener in the step that is
 * going to need it. It is to enable this usage that the resource implements
 * {@link StepListener}.
 * 
 * @author Tomas Slanina
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see Resource
 */
public class StepExecutionProxyResource extends StepListenerSupport implements Resource, ResourceLoaderAware,
		StepListener {

	private static final String JOB_NAME_PATTERN = "%JOB_NAME%";

	private static final String STEP_NAME_PATTERN = "%STEP_NAME%";

	private static final String DEFAULT_PATTERN = "data/%JOB_NAME%/" + "%STEP_NAME%.txt";

	private String filePattern = DEFAULT_PATTERN;

	private JobParametersFactory jobParametersFactory = new DefaultJobParametersFactory();

	private ResourceLoader resourceLoader = new FileSystemResourceLoader();

	private Resource delegate;

	/**
	 * @param relativePath
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#createRelative(java.lang.String)
	 */
	public Resource createRelative(String relativePath) throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.createRelative(relativePath);
	}

	/**
	 * @return
	 * @see org.springframework.core.io.Resource#exists()
	 */
	public boolean exists() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.exists();
	}

	/**
	 * @return
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	public String getDescription() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getDescription();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getFile()
	 */
	public File getFile() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getFile();
	}

	/**
	 * @return
	 * @see org.springframework.core.io.Resource#getFilename()
	 */
	public String getFilename() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getFilename();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.InputStreamSource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getInputStream();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getURI()
	 */
	public URI getURI() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getURI();
	}

	/**
	 * @return
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getURL()
	 */
	public URL getURL() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getURL();
	}

	/**
	 * @return
	 * @see org.springframework.core.io.Resource#isOpen()
	 */
	public boolean isOpen() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.isOpen();
	}

	/**
	 * @see org.springframework.core.io.Resource#isReadable()
	 */
	public boolean isReadable() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.isReadable();
	}

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
	 * @param jobName
	 * @param stepName
	 * @param properties
	 */
	private String createFileName(String jobName, String stepName, Properties properties) {
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

	/**
	 * Collect the properties of the enclosing {@link StepExecution} that will
	 * be needed to create a file name.
	 * 
	 * @see org.springframework.batch.core.domain.StepListener#beforeStep(org.springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution execution) {
		String stepName = execution.getStepName();
		String jobName = execution.getJobExecution().getJobInstance().getJobName();
		Properties properties = jobParametersFactory.getProperties(execution.getJobExecution().getJobInstance()
				.getJobParameters());
		delegate = resourceLoader.getResource(createFileName(jobName, stepName, properties));
	}

}
