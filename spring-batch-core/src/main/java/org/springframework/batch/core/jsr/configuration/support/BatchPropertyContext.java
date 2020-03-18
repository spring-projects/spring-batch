/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr.configuration.support;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.Assert;

/**
 * <p>
 * Context object to hold parsed JSR-352 batch properties, mapping properties to beans /
 * "batch artifacts". Used internally when parsing property tags from a batch configuration
 * file and to obtain corresponding values when injecting into batch artifacts.
 * </p>
 *
 * @author Chris Schaefer
 * @author Michael Minella
 * @since 3.0
 */
public class BatchPropertyContext {
	private static final String PARTITION_INDICATOR = ":partition";

	private Properties jobProperties = new Properties();
	private Map<String, Properties> stepProperties = new HashMap<>();
	private Map<String, Properties> artifactProperties = new HashMap<>();
	private Map<String, Map<String, Properties>> stepArtifactProperties = new HashMap<>();

	/**
	 * <p>
	 * Obtains the Job level properties.
	 * </p>
	 *
	 * @return the Job level properties
	 */
	public Properties getJobProperties() {
		return jobProperties;
	}

	/**
	 * <p>
	 * Adds Job level properties to the context.
	 * </p>
	 *
	 * @param properties the job {@link Properties} to add
	 */
	public void setJobProperties(Properties properties) {
		Assert.notNull(properties, "Job properties cannot be null");
		this.jobProperties.putAll(properties);
	}

	/**
	 * <p>
	 * Obtains the Step level properties for the provided Step name.
	 * </p>
	 *
	 * @param stepName the Step name to obtain properties for
	 * @return the {@link Properties} for the Step
	 */
	public Properties getStepProperties(String stepName) {
		Assert.hasText(stepName, "Step name must be provided");
		Properties properties = new Properties();

		if(stepProperties.containsKey(stepName)) {
			properties.putAll(stepProperties.get(stepName));
		}

		if(stepName.contains(PARTITION_INDICATOR)) {
			String parentStepName = stepName.substring(0, stepName.indexOf(PARTITION_INDICATOR));
			properties.putAll(getStepProperties(parentStepName));
		}

		return properties;
	}

	/**
	 * <p>
	 * Adds Step level properties to the context.
	 * </p>
	 *
	 * @param properties the step {@link Properties} to add
	 */
	public void setStepProperties(Map<String, Properties> properties) {
		Assert.notNull(properties, "Step properties cannot be null");

		for(Map.Entry<String, Properties> propertiesEntry : properties.entrySet()) {
			String stepName = propertiesEntry.getKey();
			Properties stepProperties = propertiesEntry.getValue();

			if (!stepProperties.isEmpty()) {
				if (this.stepProperties.containsKey(stepName)) {
					Properties existingStepProperties = this.stepProperties.get(stepName);

					Enumeration<?> stepPropertyNames = stepProperties.propertyNames();

					while(stepPropertyNames.hasMoreElements()) {
						String propertyEntryName = (String) stepPropertyNames.nextElement();
						existingStepProperties.put(propertyEntryName, stepProperties.getProperty(propertyEntryName));
					}

					this.stepProperties.put(stepName, existingStepProperties);
				} else {
					this.stepProperties.put(stepName, propertiesEntry.getValue());
				}
			}
		}
	}

	/**
	 * <p>
	 * Convenience method to set step level properties. Simply wraps the provided parameters
	 * and delegates to {@link #setStepProperties(java.util.Map)}.
	 * </p>
	 *
	 * @param stepName the step name to set {@link Properties} for
	 * @param properties the {@link Properties} to set
	 */
	public void setStepProperties(String stepName, Properties properties) {
		Assert.hasText(stepName, "Step name must be provided");
		Assert.notNull(properties, "Step properties must not be null");

		Map<String, Properties> stepProperties = new HashMap<>();
		stepProperties.put(stepName, properties);

		setStepProperties(stepProperties);
	}

	/**
	 * <p>
	 * Obtains the batch {@link Properties} for the provided artifact name.
	 * </p>
	 *
	 * @param artifactName the batch artifact to obtain properties for
	 * @return the {@link Properties} for the provided batch artifact
	 */
	public Properties getArtifactProperties(String artifactName) {
		Properties properties = new Properties();

		if (artifactProperties.containsKey(artifactName)) {
			properties.putAll(artifactProperties.get(artifactName));
		}

		return properties;
	}

	/**
	 * <p>
	 * Adds non-step artifact properties to the context.
	 * </p>
	 *
	 * @param properties the artifact {@link Properties} to add
	 */
	public void setArtifactProperties(Map<String, Properties> properties) {
		Assert.notNull(properties, "Step properties cannot be null");

		for(Map.Entry<String, Properties> propertiesEntry : properties.entrySet()) {
			String artifactName = propertiesEntry.getKey();
			Properties artifactProperties = propertiesEntry.getValue();

			if(!artifactProperties.isEmpty()) {
				this.artifactProperties.put(artifactName, artifactProperties);
			}
		}
	}

	/**
	 * <p>
	 * Obtains the batch {@link Properties} for the provided Step and artifact name.
	 * </p>
	 *
	 * @param stepName the Step name the artifact is associated with
	 * @param artifactName the artifact name to obtain {@link Properties} for
	 * @return the {@link Properties} for the provided Step artifact
	 */
	public Properties getStepArtifactProperties(String stepName, String artifactName) {
		Properties properties = new Properties();
		properties.putAll(getStepProperties(stepName));

		Map<String, Properties> artifactProperties = stepArtifactProperties.get(stepName);

		if (artifactProperties != null && artifactProperties.containsKey(artifactName)) {
			properties.putAll(artifactProperties.get(artifactName));
		}

		if(stepName.contains(PARTITION_INDICATOR)) {
			String parentStepName = stepName.substring(0, stepName.indexOf(PARTITION_INDICATOR));
			properties.putAll(getStepProperties(parentStepName));

			Map<String, Properties> parentArtifactProperties = stepArtifactProperties.get(parentStepName);

			if (parentArtifactProperties != null && parentArtifactProperties.containsKey(artifactName)) {
				properties.putAll(parentArtifactProperties.get(artifactName));
			}
		}

		return properties;
	}

	/**
	 * <p>
	 * Adds Step artifact properties to the context.
	 * </p>
	 *
	 * @param properties the step artifact {@link Properties} to add
	 */
	@SuppressWarnings("serial")
	public void setStepArtifactProperties(Map<String, Map<String, Properties>> properties) {
		Assert.notNull(properties, "Step artifact properties cannot be null");

		for(Map.Entry<String, Map<String, Properties>> propertyEntries : properties.entrySet()) {
			String stepName = propertyEntries.getKey();

			for(Map.Entry<String, Properties> artifactEntries : propertyEntries.getValue().entrySet()) {
				final String artifactName = artifactEntries.getKey();
				final Properties props = artifactEntries.getValue();

				Map<String, Properties> artifactProperties = stepArtifactProperties.get(stepName);

				if (artifactProperties == null) {
					stepArtifactProperties.put(stepName, new HashMap<String, Properties>() {{
						put(artifactName, props);
					}});
				} else {
					artifactProperties.put(artifactName, props);
				}
			}
		}
	}
}
