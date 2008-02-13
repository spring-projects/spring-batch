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

package org.springframework.batch.item;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.util.Assert;

/**
 * Value object representing a context for an {@link ItemStream}. It is
 * essentially a thin wrapper for a map that allows for type safety on reads. It
 * also allows for dirty checking by setting a 'dirty' flag whenever any put is
 * called.
 * 
 * @author Lucas Ward
 */
public class ExecutionAttributes {

	private boolean dirty = false;

	private final Map map;

	public ExecutionAttributes() {
		map = new HashMap();
	}
	
	public ExecutionAttributes(Map map){
		this.map = map;
	}

	public void putString(String key, String value) {

		Assert.notNull(value);
		put(key, value);
	}

	public void putLong(String key, long value) {

		put(key, new Long(value));
	}

	public void putDouble(String key, double value) {

		put(key, new Double(value));
	}
	
	public void put(String key, Object value){
		Assert.isInstanceOf(Serializable.class, value, "Value: [ " + value + "must be serializable.");
		dirty = true;
		map.put(key, value);
	}

	public boolean isDirty() {
		return dirty;
	}

	public String getString(String key) {

		return (String) readAndValidate(key, String.class);
	}

	public long getLong(String key) {

		return ((Long) readAndValidate(key, Long.class)).longValue();
	}
	
	public double getDouble(String key){
		return ((Double)readAndValidate(key, Double.class)).doubleValue();
	}
	
	public Object get(String key){
		
		return map.get(key);
	}

	private Object readAndValidate(String key, Class type) {

		Object value = map.get(key);

		if (!type.isInstance(value)) {
			throw new ClassCastException("Value for key=[" + key + "] is not of type: [" + type
					+ "], it is [" + (value == null ? null : value) + "]");
		}

		return value;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public void clearDirtyFlag() {
		dirty = false;
	}

	public Set entrySet() {
		return map.entrySet();
	}

	public boolean containsKey(String key) {
		return map.containsKey(key);
	}

	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	public Properties getProperties() {

		Properties props = new Properties();
		for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			props.setProperty(entry.getKey().toString(), entry.getValue().toString());
		}

		return props;
	}
	
	public boolean equals(Object obj) {
		if(obj instanceof ExecutionAttributes == false){
			return false;
		}
		if(this == obj){
			return true;
		}
		ExecutionAttributes rhs = (ExecutionAttributes)obj;
		return this.entrySet().equals(rhs.entrySet());
	}
	
	public int hashCode() {
		return map.hashCode();
	}
	
	public String toString() {
		return map.toString();
	}

}
