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

import java.util.HashMap;
import java.util.List;
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
	private Map<String, Properties> stepProperties = new HashMap<String, Properties>();
	private Map<String, Properties> artifactProperties = new HashMap<String, Properties>();
	private Map<String, Map<String, Properties>> stepArtifactProperties = new HashMap<String, Map<String, Properties>>();

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
	 * Obtains the Step level properties for the provided Step name.
	 * </p>
	 *
	 * @param stepName the Step name to obtain properties for
	 * @return the {@link Properties} for the Step
	 */
	public Properties getStepProperties(String stepName) {
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
	 * Obtains the batch {@link Properties} for the provided artifact name. The returned {@link Properties}
	 * will also contain any job level properties that have been set. Job level properties will not override
	 * existing lower level artifact properties.
	 * </p>
	 *
	 * @param artifactName the batch artifact to obtain properties for
	 * @return the {@link Properties} for the provided batch artifact
	 */
	public Properties getArtifactProperties(String artifactName) {
		Properties properties = new Properties();
		properties.putAll(getJobProperties());

		if (artifactProperties.containsKey(artifactName)) {
			properties.putAll(artifactProperties.get(artifactName));
		}

		return properties;
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
		properties.putAll(getJobProperties());
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
	 * Adds Job level properties to the context.
	 * </p>
	 *
	 * @param batchPropertyContextEntries the {@link BatchPropertyContextEntry} objects to add
	 */
	public void setJobPropertiesContextEntry(List<BatchPropertyContextEntry> batchPropertyContextEntries) {
		for(BatchPropertyContextEntry batchPropertyContextEntry : batchPropertyContextEntries) {
			Properties jobProperties = batchPropertyContextEntry.getProperties();

			if (jobProperties != null && !jobProperties.isEmpty()) {
				this.jobProperties.putAll(jobProperties);
			}
		}
	}

	/**
	 * <p>
	 * Adds Step level properties to the context.
	 * </p>
	 *
	 * @param batchPropertyContextEntries the {@link BatchPropertyContextEntry} objects to add
	 */
	public void setStepPropertiesContextEntry(List<BatchPropertyContextEntry> batchPropertyContextEntries) {
		for (BatchPropertyContextEntry batchPropertyContextEntry : batchPropertyContextEntries) {
			Assert.hasText(batchPropertyContextEntry.getArtifactName(), "Step name must be defined as the artifact name.");

			String stepName = batchPropertyContextEntry.getArtifactName();
			Properties stepProperties = batchPropertyContextEntry.getProperties();

			if (stepProperties != null && ! stepProperties.isEmpty()) {
				if (this.stepProperties.containsKey(stepName)) {
					Properties existingStepProperties = this.stepProperties.get(stepName);
					existingStepProperties.putAll(stepProperties);

					this.stepProperties.put(stepName, existingStepProperties);
				} else {
					this.stepProperties.put(stepName, stepProperties);
				}
			}
		}
	}

	/**
	 * <p>
	 * Adds non-Step scoped artifact properties to the context.
	 * </p>
	 *
	 * @param batchPropertyContextEntries the {@link BatchPropertyContextEntry} objects to add
	 */
	public void setArtifactPropertiesContextEntry(List<BatchPropertyContextEntry> batchPropertyContextEntries) {
		for (BatchPropertyContextEntry batchPropertyContextEntry : batchPropertyContextEntries) {
			Assert.hasText(batchPropertyContextEntry.getArtifactName(), "Artifact name must be defined");

			Properties properties = batchPropertyContextEntry.getProperties();
			String artifactName = batchPropertyContextEntry.getArtifactName();

			if (properties != null && !properties.isEmpty()) {
				artifactProperties.put(artifactName, properties);
			}
		}
	}

	/**
	 * <p>
	 * Adds Step scoped artifact properties to the context.
	 * </p>
	 *
	 * @param batchPropertyContextEntries the {@link BatchPropertyContextEntry} objects to add
	 */
	@SuppressWarnings("serial")
	public void setStepArtifactPropertiesContextEntry(List<BatchPropertyContextEntry> batchPropertyContextEntries) {
		for (BatchPropertyContextEntry batchPropertyContextEntry : batchPropertyContextEntries) {
			Assert.hasText(batchPropertyContextEntry.getStepName(), "Step name must be defined");
			Assert.hasText(batchPropertyContextEntry.getArtifactName(), "Artifact name must be defined");

			String stepName = batchPropertyContextEntry.getStepName();
			final String artifactName = batchPropertyContextEntry.getArtifactName();

			final Properties properties = batchPropertyContextEntry.getProperties();

			if (!properties.isEmpty()) {
				Map<String, Properties> artifactProperties = stepArtifactProperties.get(stepName);

				if (artifactProperties == null) {
					stepArtifactProperties.put(stepName, new HashMap<String, Properties>() {{
						put(artifactName, properties);
					}});
				} else {
					artifactProperties.put(artifactName, properties);
				}
			}
		}
	}

	/**
	 * <p>
	 * Obtains the property to be used when setting data for the provided {@link BatchArtifact.BatchArtifactType}.
	 * </p>
	 *
	 * @param batchArtifactType the {@link BatchArtifact.BatchArtifactType} to lookup the property name for
	 * @return the property name for
	 * @throws IllegalStateException if an unhandled {@link BatchArtifact.BatchArtifactType} is encountered
	 */
	public String getPropertyName(BatchArtifact.BatchArtifactType batchArtifactType) {
		switch (batchArtifactType) {
		case STEP:
			return "stepPropertiesContextEntry";
		case STEP_ARTIFACT:
			return "stepArtifactPropertiesContextEntry";
		case ARTIFACT:
			return "artifactPropertiesContextEntry";
		case JOB:
			return "jobPropertiesContextEntry";
		default:
			throw new IllegalStateException("Unhandled BatchArtifactType of: " + batchArtifactType);
		}
	}

	/**
	 * <p>
	 * Simple object to encapsulate batch properties of a given batch artifact.
	 * </p>
	 *
	 * @author Chris Schaefer
	 * @since 3.0
	 */
	public class BatchPropertyContextEntry {
		private String stepName;
		private String artifactName;
		private Properties properties;
		private BatchArtifact.BatchArtifactType batchArtifactType;

		/**
		 * <p>
		 * Creates a new entry instance using the provided bean name representing batch artifact
		 * and its associated {@link Properties}.
		 * </p>
		 *
		 * @param artifactName the name representing the batch artifact
		 * @param properties the associated {@link Properties}
		 * @param batchArtifactType the associated {@link BatchArtifact.BatchArtifactType}
		 */
		public BatchPropertyContextEntry(String artifactName, Properties properties, BatchArtifact.BatchArtifactType batchArtifactType) {
			this.artifactName = artifactName;
			this.batchArtifactType = batchArtifactType;
			this.properties = properties != null ? properties : new Properties();
		}

		/**
		 * <p>
		 * Obtains the name of the batch artifact this entry is associated with.
		 * </p>
		 *
		 * @return the name of the batch artifact
		 */
		public String getArtifactName() {
			return artifactName;
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

		/**
		 * <p>
		 * Obtains the {@link BatchArtifact.BatchArtifactType} represented by this context entry.
		 * </p>
		 *
		 * @return the {@link BatchArtifact.BatchArtifactType}
		 */
		public BatchArtifact.BatchArtifactType getBatchArtifactType() {
			return batchArtifactType;
		}

		/**
		 * <p>
		 * Sets the Step name associated with this entry.
		 * </p>
		 *
		 * @param stepName the Step name associated with this entry
		 */
		public void setStepName(String stepName) {
			this.stepName = stepName;
		}

		/**
		 * <p>
		 * Obtains the Step name associated with this entry.
		 * </p>
		 *
		 * @return the step name associated with this entry
		 */
		public String getStepName() {
			return stepName;
		}
	}
}
