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
package org.springframework.batch.core.jsr.configuration.support;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <p>
 * Simple context object to hold parsed JSR-352 batch properties, mapping properties
 * to beans / "batch artifacts". Used internally when parsing property tags from a batch
 * configuration file and to obtain corresponding values when injecting into batch artifacts.
 * </p>
 *
 * @author Chris Schaefer
 * @author Michael Minella
 * @since 3.0
 */
public class BatchPropertyContext {
	private static final String JOB_ARTIFACT_PROPERTY_PREFIX = "job-";
	private static final Pattern JOB_PATH_DELIMITER_PATTERN = Pattern.compile("\\.");

	private ConcurrentHashMap<String, Properties> batchProperties = new ConcurrentHashMap<String, Properties>();

	/**
	 * <p>
	 * Adds each of the provided {@link BatchPropertyContext} objects to the existing property
	 * context.
	 * </p>
	 *
	 * @param batchPropertyContextEntries the {@link BatchPropertyContextEntry} objects to add
	 */
	public void setBatchContextEntries(List<BatchPropertyContextEntry> batchPropertyContextEntries) {
		for (BatchPropertyContextEntry batchPropertyContextEntry : batchPropertyContextEntries) {
			setBatchContextEntry(batchPropertyContextEntry);
		}
	}

	private void setBatchContextEntry(BatchPropertyContextEntry batchPropertyContextEntry) {
		String beanName = batchPropertyContextEntry.getBeanName();
		Properties properties = batchPropertyContextEntry.getProperties();

		if (batchProperties.containsKey(beanName)) {
			Properties existingProperties = batchProperties.get(beanName);
			existingProperties.putAll(properties);

			batchProperties.put(beanName, existingProperties);
		} else {
			batchProperties.put(beanName, properties);
		}
	}

	/**
	 * <p>
	 * Obtains all properties for the specific batch artifact / bean name without any job level
	 * properties.
	 * </p>
	 *
	 * @param beanName the batch artifact / bean name to obtain {@link Properties} for
	 * @return the step level {@link Properties}
	 */
	public Properties getStepLevelProperties(String beanName) {
		Properties properties = new Properties();

		if (batchProperties.containsKey(beanName)) {
			properties.putAll(batchProperties.get(beanName));
		}

		return properties;
	}

	/**
	 * <p>
	 * Obtains the batch {@link Properties} for the provided bean name / batch artifact. The returned
	 * {@link Properties} will also contain any job level properties that have been set. Job level
	 * properties will not override existing lower level artifact properties.
	 * </p>
	 *
	 * @param beanName the bean name representing the batch artifact to obtain properties for
	 * @return the {@link Properties} for the provided batch artifact
	 */
	public Properties getBatchProperties(String beanName) {
		Properties properties = new Properties();

		if (batchProperties.containsKey(beanName)) {
			properties.putAll(batchProperties.get(beanName));
		}

		Properties jobLevelProperties = getJobProperties();

		for (String jobLevelProperty : jobLevelProperties.stringPropertyNames()) {
			if (!properties.containsKey(jobLevelProperty)) {
				properties.put(jobLevelProperty, jobLevelProperties.getProperty(jobLevelProperty));
			}
		}

		return properties;
	}

	/**
	 * <p>
	 * Obtains all {@link Properties} that reside at the Job level configuration.
	 * </p>
	 *
	 * @return the job level {@link Properties}
	 */
	public Properties getJobProperties() {
		Properties jobProperties = new Properties();

		for (String jobLevelProperty : batchProperties.keySet()) {
			if(isJobLevelComponentPath(jobLevelProperty)) {
				if (batchProperties.containsKey(jobLevelProperty)) {
					jobProperties.putAll(batchProperties.get(jobLevelProperty));
					break;
				}
			}
		}

		return jobProperties;
	}

	// for now we assume properties are using a path format, currently: jobName.componentName.artifactName
	// componentName can be the step name, job name prefixed by job- etc
	protected boolean isJobLevelComponentPath(String jobLevelProperty) {
		if(jobLevelProperty == null || "".equals(jobLevelProperty)) {
			return false;
		}

		String[] path = JOB_PATH_DELIMITER_PATTERN.split(jobLevelProperty);

		if (path.length >= 2) {
			String componentPath = path[1];

			if (componentPath != null && componentPath.startsWith(JOB_ARTIFACT_PROPERTY_PREFIX)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * <p>
	 * Simple object to encapsulate batch properties of a given bean / batch artifact.
	 * </p>
	 *
	 * @author Chris Schaefer
	 * @since 3.0
	 */
	public class BatchPropertyContextEntry {
		private String beanName;
		private Properties properties;

		/**
		 * <p>
		 * Creates a new entry instance using the provided bean name representing batch artifact
		 * and its associated {@link Properties}.
		 * </p>
		 *
		 * @param beanName the bean name representing the batch artifact
		 * @param properties the associated {@link Properties}
		 */
		public BatchPropertyContextEntry(String beanName, Properties properties) {
			this.beanName = beanName;
			this.properties = properties;
		}

		/**
		 * <p>
		 * Obtains the bean name of the batch artifact this entry is associated with.
		 * </p>
		 *
		 * @return the bean name of the batch artifact
		 */
		public String getBeanName() {
			return beanName;
		}

		/**
		 * <p>
		 * Obtains the batch {@link Properties} that are associated with this entry.
		 * </p>
		 *
		 * @return the batch {@link Properties}
		 */
		public Properties getProperties() {
			return properties;
		}
	}
}
