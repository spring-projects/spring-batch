package org.springframework.batch.execution.tasklet.support;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;

/**
 * Runs a collection of ItemProcessors in fixed-order sequence.
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemProcessor implements ItemProcessor, Restartable, StatisticsProvider {
	
	private static final String SEPARATOR = "#";

	private List itemProcessors;
	
	/**
	 * Calls injected ItemProcessors in order.
	 */
	public void process(Object data) throws Exception {
		for (Iterator iterator = itemProcessors.listIterator(); iterator.hasNext();) {
			((ItemProcessor) iterator.next()).process(data);
		}
	}

	/**
	 * Compound restart data of all injected (Restartable) ItemProcessors, property keys are
	 * prefixed with list index of the ItemProcessor.
	 */
	public RestartData getRestartData() {
		Properties props = createCompoundProperties(new PropertiesExtractor() {
			public Properties extractProperties(Object o) {
				if (o instanceof Restartable) {
					return ((Restartable)o).getRestartData().getProperties();
				} else {
					return null;
				}
			}
		});
		return new GenericRestartData(props);
	}

	/**
	 * @param data contains values of restart data, property keys are expected to be prefixed with
	 * list index of the ItemProcessor.
	 */
	public void restoreFrom(RestartData data) {
		if (data == null || data.getProperties() == null) {
			// do nothing
			return;
		}
		
		List restartDataList = parseProperties(data.getProperties());
		
		// iterators would make the loop below less readable
		for (int i=0; i < itemProcessors.size(); i++) {
			if (itemProcessors.get(i) instanceof Restartable) {
				((Restartable) itemProcessors.get(i)).restoreFrom((RestartData) restartDataList.get(i));
			}
		}
		
	}

	/**
	 * @return Properties containing statistics of all injected ItemProcessors,
	 * property keys are prefixed with the list index of the ItemProcessor.
	 */
	public Properties getStatistics() {
		return createCompoundProperties(new PropertiesExtractor() {
			public Properties extractProperties(Object o) {
				if (o instanceof StatisticsProvider){
					return ((StatisticsProvider) o).getStatistics();
				} else {
					return null;
				}
			}
		});
	}
	
	public void setItemProcessors(List itemProcessors) {
		this.itemProcessors = itemProcessors;
	}

	/**
	 * Parses compound properties into a list of RestartData.
	 */
	private List parseProperties(Properties props) {
		List restartDataList = new ArrayList(itemProcessors.size());
		for (int i = 0; i<itemProcessors.size(); i++) {
			restartDataList.add(new GenericRestartData(new Properties()));
		}
		
		for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry entry = (Map.Entry) iterator.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			int separatorIndex = key.indexOf(SEPARATOR);
			int i = Integer.valueOf(key.substring(0, separatorIndex)).intValue();
			((RestartData)restartDataList.get(i)).getProperties().setProperty(
					key.substring(separatorIndex + 1), value);
		}
		return restartDataList;
	}
	
	/**
	 * @param extractor used to extract Properties from ItemProviders
	 * @return compound Properties containing all the Properties from injected ItemProcessors
	 * with property keys prefixed by list index.
	 */
	private Properties createCompoundProperties(PropertiesExtractor extractor) {
		Properties stats = new Properties();
		int index = 0;
		for (Iterator iterator = itemProcessors.listIterator(); iterator.hasNext();) {
			ItemProcessor processor = (ItemProcessor) iterator.next();
			Properties processorStats = extractor.extractProperties(processor);
			if (processorStats != null) { 
				for (Iterator iterator2 = processorStats.entrySet().iterator(); iterator2.hasNext();) {
					Map.Entry entry = (Map.Entry) iterator2.next();
					stats.setProperty("" + index + SEPARATOR + entry.getKey(), (String) entry.getValue());
				}
			}
			index++;
		}
		return stats;
	}
	
	/**
	 * Extracts information from given object in the form of {@link Properties}. If the information
	 * is not available (e.g. unexpected object class) return null.
	 */
	private interface PropertiesExtractor {
		Properties extractProperties(Object o);
	}

}
