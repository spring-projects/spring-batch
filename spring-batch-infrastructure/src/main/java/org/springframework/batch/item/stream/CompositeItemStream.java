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
package org.springframework.batch.item.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.StreamContext;

/**
 * @author Dave Syer
 * 
 */
public class CompositeItemStream implements ItemStream {

	private static final String SEPARATOR = "#";

	private List delegates;

	public void setDelegates(List delegates) {
		this.delegates = delegates;
	}

	/**
	 * Compound restart data of all injected (Restartable) ItemProcessors,
	 * property keys are prefixed with list index of the ItemProcessor.
	 */
	public StreamContext getRestartData() {
		Properties props = createCompoundProperties(new PropertiesExtractor() {
			public Properties extractProperties(Object o) {
				if (o instanceof ItemStream) {
					return ((ItemStream) o).getRestartData().getProperties();
				}
				else {
					return null;
				}
			}
		});
		return new GenericStreamContext(props);
	}

	/**
	 * @param data contains values of restart data, property keys are expected
	 * to be prefixed with list index of the ItemProcessor.
	 */
	public void restoreFrom(StreamContext data) {
		if (data == null || data.getProperties() == null) {
			// do nothing
			return;
		}

		List restartDataList = parseProperties(data.getProperties());

		// iterators would make the loop below less readable
		for (int i = 0; i < delegates.size(); i++) {
			if (delegates.get(i) instanceof ItemStream) {
				((ItemStream) delegates.get(i)).restoreFrom((StreamContext) restartDataList.get(i));
			}
		}

	}

	/**
	 * Parses compound properties into a list of RestartData.
	 */
	private List parseProperties(Properties props) {
		List restartDataList = new ArrayList(delegates.size());
		for (int i = 0; i < delegates.size(); i++) {
			restartDataList.add(new GenericStreamContext(new Properties()));
		}

		for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			int separatorIndex = key.indexOf(SEPARATOR);
			int i = Integer.valueOf(key.substring(0, separatorIndex)).intValue();
			((StreamContext) restartDataList.get(i)).getProperties().setProperty(key.substring(separatorIndex + 1),
					value);
		}
		return restartDataList;
	}

	/**
	 * @param extractor used to extract Properties from {@link ItemReader}s
	 * @return compound Properties containing all the Properties from injected
	 * delegates with property keys prefixed by list index.
	 */
	private Properties createCompoundProperties(PropertiesExtractor extractor) {
		Properties stats = new Properties();
		int index = 0;
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			Properties writerStats = extractor.extractProperties(iterator.next());
			if (writerStats != null) {
				for (Iterator iterator2 = writerStats.entrySet().iterator(); iterator2.hasNext();) {
					Map.Entry entry = (Map.Entry) iterator2.next();
					stats.setProperty("" + index + SEPARATOR + entry.getKey(), (String) entry.getValue());
				}
			}
			index++;
		}
		return stats;
	}

	/**
	 * Extracts information from given object in the form of {@link Properties}.
	 * If the information is not available (e.g. unexpected object class) return
	 * null.
	 */
	private interface PropertiesExtractor {
		Properties extractProperties(Object o);
	}

	public void close() throws Exception {
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			Object delegate = iterator.next();
			if (delegate instanceof ItemStream) {
				((ItemStream) delegate).close();
			}
		}
	}

	public void open() throws Exception {
		for (Iterator iterator = delegates.listIterator(); iterator.hasNext();) {
			Object delegate = iterator.next();
			if (delegate instanceof ItemStream) {
				((ItemStream) delegate).open();
			}
		}
	}

	/**
	 * Public getter for the list of delegates.
	 * @return the delegates
	 */
	public List getDelegates() {
		return delegates;
	}

}
