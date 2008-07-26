/**
 * 
 */
package org.springframework.batch.core;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Lucas Ward
 *
 */
public class JobParametersPrototype {

	private final Map<String, ? extends Object> parameters;
	
	
	public JobParametersPrototype(Map<String, Object> parameters) {
		
		for(Entry<String, Object> entry : parameters.entrySet()){
			Object parameter = entry.getValue();
			String key = entry.getKey();
			Class<?> type = parameter.getClass();
			if (!type.isInstance(String.class) || !type.isInstance(Long.class) || 
					!type.isInstance(Double.class) || !type.isInstance(Date.class)) {
				throw new ClassCastException("Value for key=[" + key + "] is not of type: [ String, Long, Double, or Date], it is ["
						+ (parameter == null ? null : "(" + type + ")" + parameter) + "]");
			}
		}
		
		this.parameters = new LinkedHashMap<String, Object>(parameters);
	}
	
	public long getLong(String key){
		return ((Long)parameters.get(key)).longValue();
	}
	
	public String getString(String key){
		return parameters.get(key).toString();
	}
	
	public Double getDouble(String key){
		return ((Double)parameters.get(key)).doubleValue();
	}
	
	public Date getDate(String key){
		return (Date)parameters.get(key);
	}
	
	public Map<String, ? extends Object> getParameters(){
		return new LinkedHashMap<String, Object>(parameters);
	}
	
}
