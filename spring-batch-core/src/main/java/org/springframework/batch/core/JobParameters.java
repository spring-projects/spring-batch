/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.core;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.lang.Nullable;

/**
 * Value object representing runtime parameters to a batch job. Because the
 * parameters have no individual meaning outside of the JobParameters they are
 * contained within, it is a value object rather than an entity. It is also
 * extremely important that a parameters object can be reliably compared to
 * another for equality, in order to determine if one JobParameters object
 * equals another. Furthermore, because these parameters will need to be
 * persisted, it is vital that the types added are restricted.
 * 
 * This class is immutable and therefore thread-safe.
 * 
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 1.0
 */
@SuppressWarnings("serial")
public class JobParameters implements Serializable {

	private final Map<String,JobParameter> parameters;

	public JobParameters() {
		this.parameters = new LinkedHashMap<>();
	}

	public JobParameters(Map<String,JobParameter> parameters) {
		this.parameters = new LinkedHashMap<>(parameters);
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.
	 * 
	 * @param key The key to get a value for
	 * @return The <code>Long</code> value or {@code null} if the key is absent
	 */
	@Nullable
	public Long getLong(String key){
		if (!parameters.containsKey(key)) {
			return null;
		}
		Object value = parameters.get(key).getValue();
		return value==null ? null : ((Long)value).longValue();
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 * 
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 * @deprecated Use {@link JobParameters#getLong(java.lang.String, java.lang.Long)}
	 * instead. This method will be removed in a future release.
	 */
	@Deprecated
	public Long getLong(String key, long defaultValue){
		if(parameters.containsKey(key)){
			return getLong(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	@Nullable
	public Long getLong(String key, @Nullable Long defaultValue){
		if(parameters.containsKey(key)){
			return getLong(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the String represented by the provided key.
	 * 
	 * @param key The key to get a value for
	 * @return The <code>String</code> value or {@code null} if the key is absent
	 */
	@Nullable
	public String getString(String key){
		JobParameter value = parameters.get(key);
		return value==null ? null : value.toString();
	}

	/**
	 * Typesafe Getter for the String represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 * 
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	@Nullable
	public String getString(String key, @Nullable String defaultValue){
		if(parameters.containsKey(key)){
			return getString(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.
	 * 
	 * @param key The key to get a value for
	 * @return The <code>Double</code> value or {@code null} if the key is absent
	 */
	@Nullable
	public Double getDouble(String key){
		if (!parameters.containsKey(key)) {
			return null;
		}
		Double value = (Double)parameters.get(key).getValue();
		return value==null ? null : value.doubleValue();
	}

	/**
	 * Typesafe Getter for the Double represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 * 
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 * @deprecated Use {@link JobParameters#getDouble(java.lang.String, java.lang.Double)}
	 * instead. This method will be removed in a future release.
	 */
	@Deprecated
	public Double getDouble(String key, double defaultValue){
		if(parameters.containsKey(key)){
			return getDouble(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Double represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 *
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	@Nullable
	public Double getDouble(String key, @Nullable Double defaultValue){
		if(parameters.containsKey(key)){
			return getDouble(key);
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Typesafe Getter for the Date represented by the provided key.
	 * 
	 * @param key The key to get a value for
	 * @return The <code>java.util.Date</code> value or {@code null} if the key
	 * is absent
	 */
	@Nullable
	public Date getDate(String key){
		return this.getDate(key,null);
	}

	/**
	 * Typesafe Getter for the Date represented by the provided key.  If the
	 * key does not exist, the default value will be returned.
	 * 
	 * @param key to return the value for
	 * @param defaultValue to return if the value doesn't exist
	 * @return the parameter represented by the provided key, defaultValue
	 * otherwise.
	 */
	@Nullable
	public Date getDate(String key, @Nullable Date defaultValue){
		if(parameters.containsKey(key)){
			return (Date)parameters.get(key).getValue();
		}
		else{
			return defaultValue;
		}
	}

	/**
	 * Get a map of all parameters, including string, long, and date.
	 * 
	 * @return an unmodifiable map containing all parameters.
	 */
	public Map<String, JobParameter> getParameters(){
		return new LinkedHashMap<>(parameters);
	}

	/**
	 * @return true if the parameters is empty, false otherwise.
	 */
	public boolean isEmpty(){
		return parameters.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof JobParameters == false){
			return false;
		}

		if(obj == this){
			return true;
		}

		JobParameters rhs = (JobParameters)obj;
		return this.parameters.equals(rhs.parameters);
	}

	@Override
	public int hashCode() {
		return 17 + 23 * parameters.hashCode();
	}

	@Override
	public String toString() {
		return parameters.toString();
	}

	public Properties toProperties() {
		Properties props = new Properties();

		for (Map.Entry<String, JobParameter> param : parameters.entrySet()) {
			if(param.getValue() != null) {
				props.put(param.getKey(), param.getValue().toString());
			}
		}

		return props;
	}
}
