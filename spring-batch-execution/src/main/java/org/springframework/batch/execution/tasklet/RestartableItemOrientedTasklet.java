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

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.support.PropertiesConverter;

/**
 * An extension of {@link ItemOrientedTasklet} that delegates calls to
 * {@link Restartable} to the provider and processor.
 * 
 * @see ItemReader
 * @see ItemWriter
 * @see Restartable
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class RestartableItemOrientedTasklet extends ItemOrientedTasklet implements Restartable {

	/**
	 * @see Restartable#getRestartData()
	 */
	public RestartData getRestartData() {

		RestartData itemProviderRestartData = null;
		RestartData itemProcessorRestartData = null;

		if (itemProvider instanceof Restartable) {
			itemProviderRestartData = ((Restartable) itemProvider).getRestartData();
		}

		if (itemWriter instanceof Restartable) {
			itemProcessorRestartData = ((Restartable) itemWriter).getRestartData();
		}

		RestartableItemOrientedTaskletRestartData restartData = new RestartableItemOrientedTaskletRestartData(itemProviderRestartData, itemProcessorRestartData);

		return restartData;
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 */
	public void restoreFrom(RestartData data) {
		if (data == null || data.getProperties() == null)
			return;

		RestartableItemOrientedTaskletRestartData moduleRestartData;

		if (data instanceof RestartableItemOrientedTaskletRestartData) {
			moduleRestartData = (RestartableItemOrientedTaskletRestartData) data;
		}
		else {
			moduleRestartData = new RestartableItemOrientedTaskletRestartData(data.getProperties());
		}

		if (itemProvider instanceof Restartable) {
			((Restartable) itemProvider).restoreFrom(moduleRestartData.providerData);
		}
		if (itemWriter instanceof Restartable) {
			((Restartable) itemWriter).restoreFrom(moduleRestartData.processorData);
		}
	}

	private class RestartableItemOrientedTaskletRestartData implements RestartData {

		private static final String PROVIDER_KEY = "DATA_PROVIDER";

		private static final String PROCESSOR_KEY = "DATA_PROCESSOR";

		RestartData providerData;

		RestartData processorData;

		public RestartableItemOrientedTaskletRestartData(RestartData providerData, RestartData processorData) {
			this.providerData = providerData;
			this.processorData = processorData;
		}

		public RestartableItemOrientedTaskletRestartData(Properties data) {
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
