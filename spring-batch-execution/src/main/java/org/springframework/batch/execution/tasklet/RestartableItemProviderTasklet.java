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

package org.springframework.batch.execution.tasklet;

import java.util.Properties;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemProvider;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.support.PropertiesConverter;

/**
 * An extension of {@link ItemProviderProcessTasklet} that delegates calls to
 * {@link Restartable} to the provider and processor.
 * 
 * @see ItemProvider
 * @see ItemProcessor
 * @see Restartable
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class RestartableItemProviderTasklet extends ItemProviderProcessTasklet implements Restartable {

	/**
	 * @see Restartable#getRestartData()
	 */
	public RestartData getRestartData() {

		RestartData itemProviderRestartData = null;
		RestartData itemProcessorRestartData = null;

		if (itemProvider instanceof Restartable) {
			itemProviderRestartData = ((Restartable) itemProvider).getRestartData();
		}

		if (itemProcessor instanceof Restartable) {
			itemProcessorRestartData = ((Restartable) itemProcessor).getRestartData();
		}

		RestartableItemProviderTaskletRestartData restartData = new RestartableItemProviderTaskletRestartData(itemProviderRestartData, itemProcessorRestartData);

		return restartData;
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 */
	public void restoreFrom(RestartData data) {
		if (data == null || data.getProperties() == null)
			return;

		RestartableItemProviderTaskletRestartData moduleRestartData;

		if (data instanceof RestartableItemProviderTaskletRestartData) {
			moduleRestartData = (RestartableItemProviderTaskletRestartData) data;
		}
		else {
			moduleRestartData = new RestartableItemProviderTaskletRestartData(data.getProperties());
		}

		if (itemProvider instanceof Restartable) {
			((Restartable) itemProvider).restoreFrom(moduleRestartData.providerData);
		}
		if (itemProcessor instanceof Restartable) {
			((Restartable) itemProcessor).restoreFrom(moduleRestartData.processorData);
		}
	}

	private class RestartableItemProviderTaskletRestartData implements RestartData {

		private static final String PROVIDER_KEY = "DATA_PROVIDER";

		private static final String PROCESSOR_KEY = "DATA_PROCESSOR";

		RestartData providerData;

		RestartData processorData;

		public RestartableItemProviderTaskletRestartData(RestartData providerData, RestartData processorData) {
			this.providerData = providerData;
			this.processorData = processorData;
		}

		public RestartableItemProviderTaskletRestartData(Properties data) {
			providerData = new GenericRestartData(PropertiesConverter
					.stringToProperties(data.getProperty(PROVIDER_KEY)));
			processorData = new GenericRestartData(PropertiesConverter.stringToProperties(data
					.getProperty(PROCESSOR_KEY)));
		}

		public Properties getProperties() {
			Properties props = new Properties();
			if (providerData != null) {
				props.setProperty(PROVIDER_KEY, PropertiesConverter.propertiesToString(providerData.getProperties()));
			}
			if (processorData != null) {
				props.setProperty(PROCESSOR_KEY, PropertiesConverter.propertiesToString(processorData.getProperties()));
			}
			return props;
		}
	}
}
