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

package org.springframework.batch.core.resource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
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
 * start with a double forward slash "//" to resolve to an absolute directory
 * (or else use a full URL with the file: prefix).<br/>
 * 
 * To use this resource it must be initialised with a {@link StepExecution}.
 * The best way to do that is to register it as a listener in the step that is
 * going to need it. For this reason the resource implements
 * {@link StepExecutionListener}.
 * 
 * @author Tomas Slanina
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see Resource
 */
public class StepExecutionResourceProxy extends StepExecutionListenerSupport implements Resource, ResourceLoaderAware,
		StepExecutionListener {

	private static final String JOB_NAME_PATTERN = "%JOB_NAME%";

	private static final String STEP_NAME_PATTERN = "%STEP_NAME%";

	private static final String DEFAULT_PATTERN = "data/%JOB_NAME%/" + "%STEP_NAME%.txt";

	private String filePattern = DEFAULT_PATTERN;

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private ResourceLoader resourceLoader = new FileSystemResourceLoader();

	private Resource delegate;

	/**
	 * @param relativePath
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#createRelative(java.lang.String)
	 */
	public Resource createRelative(String relativePath) throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.createRelative(relativePath);
	}

	/**
	 * @see org.springframework.core.io.Resource#exists()
	 */
	public boolean exists() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.exists();
	}

	/**
	 * @see org.springframework.core.io.Resource#getDescription()
	 */
	public String getDescription() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getDescription();
	}

	/**
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getFile()
	 */
	public File getFile() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getFile();
	}

	/**
	 * @see org.springframework.core.io.Resource#getFilename()
	 */
	public String getFilename() {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getFilename();
	}

	/**
	 * @throws IOException
	 * @see org.springframework.core.io.InputStreamSource#getInputStream()
	 */
	public InputStream getInputStream() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getInputStream();
	}

	/**
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getURI()
	 */
	public URI getURI() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getURI();
	}

	/**
	 * @throws IOException
	 * @see org.springframework.core.io.Resource#getURL()
	 */
	public URL getURL() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.getURL();
	}

	/**
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
	 * @see org.springframework.core.io.Resource#lastModified()
	 */
	public long lastModified() throws IOException {
		Assert.state(delegate != null, "The delegate resource has not been initialised. "
				+ "Remember to register this object as a StepListener.");
		return delegate.lastModified();
	}

	/**
	 * Public setter for the {@link JobParametersConverter} used to translate
	 * {@link JobParameters} into {@link Properties}. Defaults to a
	 * {@link DefaultJobParametersConverter}.
	 * @param jobParametersConverter the {@link JobParametersConverter} to set
	 */
	public void setJobParametersFactory(JobParametersConverter jobParametersConverter) {
		this.jobParametersConverter = jobParametersConverter;
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
	 * @see org.springframework.batch.core.StepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	public void beforeStep(StepExecution execution) {
		String stepName = execution.getStepName();
		String jobName = execution.getJobExecution().getJobInstance().getJobName();
		Properties properties = jobParametersConverter.getProperties(execution.getJobExecution().getJobInstance()
				.getJobParameters());
		String fileName = createFileName(jobName, stepName, properties);
		if(fileName.indexOf("%") > -1){
			//if a % is still left in the fileName after matching, we have to assume that either no job parameter was found,
			//or an invalid path was used.
			throw new IllegalStateException("Invalid file pattern provided: [" + this.filePattern + "], tokens still remain after parameter matching: [" +
					fileName + "]");
		}
		delegate = resourceLoader.getResource(fileName);
	}

	/**
	 * Delegates to the proxied Resource if set, otherwise returns the value of {@link #setFilePattern(String)}.
	 */
	public String toString() {
		return (delegate == null) ? filePattern : delegate.toString(); 
	}

}
