/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.item;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.Nullable;

/**
 * Object representing a context for an {@link ItemStream}. It is a thin wrapper for a map
 * that allows optionally for type safety on reads. It also allows for dirty checking by
 * setting a 'dirty' flag whenever any put is called.
 * <p>
 * Non-transient entries should be serializable, otherwise a custom serializer should be
 * used. Note that putting <code>null</code> value is equivalent to removing the entry for
 * the given key.
 *
 * @author Lucas Ward
 * @author Douglas Kaminsky
 * @author Mahmoud Ben Hassine
 * @author Seokmun Heo
 */
public class ExecutionContext implements Serializable {

	private volatile boolean dirty = false;

	private final Map<String, Object> map;

	/**
	 * Default constructor. Initializes a new execution context with an empty internal
	 * map.
	 */
	public ExecutionContext() {
		this.map = new ConcurrentHashMap<>();
	}

	/**
	 * Initializes a new execution context with the contents of another map.
	 * @param map Initial contents of context.
	 */
	public ExecutionContext(Map<String, Object> map) {
		this.map = new ConcurrentHashMap<>(map);
	}

	/**
	 * Initializes a new {@link ExecutionContext} with the contents of another
	 * {@code ExecutionContext}.
	 * @param executionContext containing the entries to be copied to this current
	 * context.
	 */
	public ExecutionContext(ExecutionContext executionContext) {
		this();
		if (executionContext == null) {
			return;
		}
		this.map.putAll(executionContext.toMap());
	}

	/**
	 * Adds a String value to the context. Putting <code>null</code> value for a given key
	 * removes the key.
	 * @param key Key to add to context
	 * @param value Value to associate with key
	 */

	public void putString(String key, @Nullable String value) {

		put(key, value);
	}

	/**
	 * Adds a Long value to the context.
	 * @param key Key to add to context
	 * @param value Value to associate with key
	 */
	public void putLong(String key, long value) {

		put(key, value);
	}

	/**
	 * Adds an Integer value to the context.
	 * @param key Key to add to context
	 * @param value Value to associate with key
	 */
	public void putInt(String key, int value) {
		put(key, value);
	}

	/**
	 * Add a Double value to the context.
	 * @param key Key to add to context
	 * @param value Value to associate with key
	 */
	public void putDouble(String key, double value) {

		put(key, value);
	}

	/**
	 * Add an Object value to the context. Putting <code>null</code> value for a given key
	 * removes the key.
	 * @param key Key to add to context
	 * @param value Value to associate with key
	 */
	public void put(String key, @Nullable Object value) {
		if (value != null) {
			Object result = this.map.put(key, value);
			this.dirty = this.dirty || result == null || !result.equals(value);
		}
		else {
			Object result = this.map.remove(key);
			this.dirty = this.dirty || result != null;
		}
	}

	/**
	 * Indicates if context has been changed with a "put" operation since the dirty flag
	 * was last cleared. Note that the last time the flag was cleared might correspond to
	 * creation of the context. A context is only dirty if a new value is put or an old
	 * one is removed.
	 * @return True if a new value was put or an old one was removed since the last time
	 * the flag was cleared
	 */
	public boolean isDirty() {
		return this.dirty;
	}

	/**
	 * Typesafe Getter for the String represented by the provided key.
	 * @param key The key to get a value for
	 * @return The <code>String</code> value
	 */
	public String getString(String key) {

		return readAndValidate(key, String.class);
	}

	/**
	 * Typesafe Getter for the String represented by the provided key with default value
	 * to return if key is not represented.
	 * @param key The key to get a value for
	 * @param defaultString Default to return if key is not represented
	 * @return The <code>String</code> value if key is represented, specified default
	 * otherwise
	 */
	public String getString(String key, String defaultString) {
		if (!containsKey(key)) {
			return defaultString;
		}

		return getString(key);
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key.
	 * @param key The key to get a value for
	 * @return The <code>Long</code> value
	 */
	public long getLong(String key) {

		return readAndValidate(key, Long.class);
	}

	/**
	 * Typesafe Getter for the Long represented by the provided key with default value to
	 * return if key is not represented.
	 * @param key The key to get a value for
	 * @param defaultLong Default to return if key is not represented
	 * @return The <code>long</code> value if key is represented, specified default
	 * otherwise
	 */
	public long getLong(String key, long defaultLong) {
		if (!containsKey(key)) {
			return defaultLong;
		}

		return getLong(key);
	}

	/**
	 * Typesafe Getter for the Integer represented by the provided key.
	 * @param key The key to get a value for
	 * @return The <code>Integer</code> value
	 */
	public int getInt(String key) {

		return readAndValidate(key, Integer.class);
	}

	/**
	 * Typesafe Getter for the Integer represented by the provided key with default value
	 * to return if key is not represented.
	 * @param key The key to get a value for
	 * @param defaultInt Default to return if key is not represented
	 * @return The <code>int</code> value if key is represented, specified default
	 * otherwise
	 */
	public int getInt(String key, int defaultInt) {
		if (!containsKey(key)) {
			return defaultInt;
		}

		return getInt(key);
	}

	/**
	 * Typesafe Getter for the Double represented by the provided key.
	 * @param key The key to get a value for
	 * @return The <code>Double</code> value
	 */
	public double getDouble(String key) {
		return readAndValidate(key, Double.class);
	}

	/**
	 * Typesafe Getter for the Double represented by the provided key with default value
	 * to return if key is not represented.
	 * @param key The key to get a value for
	 * @param defaultDouble Default to return if key is not represented
	 * @return The <code>double</code> value if key is represented, specified default
	 * otherwise
	 */
	public double getDouble(String key, double defaultDouble) {
		if (!containsKey(key)) {
			return defaultDouble;
		}

		return getDouble(key);
	}

	/**
	 * Getter for the value represented by the provided key.
	 * @param key The key to get a value for
	 * @return The value represented by the given key or {@code null} if the key is not
	 * present
	 */
	@Nullable
	public Object get(String key) {
		return this.map.get(key);
	}

	/**
	 * Typesafe getter for the value represented by the provided key, with cast to given
	 * class.
	 * @param key The key to get a value for
	 * @param type The class of return type
	 * @param <V> Type of returned value
	 * @return The value of given type represented by the given key or {@code null} if the
	 * key is not present
	 * @since 5.1
	 */
	@Nullable
	public <V> V get(String key, Class<V> type) {
		Object value = this.map.get(key);
		if (value == null) {
			return null;
		}
		return get(key, type, null);
	}

	/**
	 * Typesafe getter for the value represented by the provided key, with cast to given
	 * class.
	 * @param key The key to get a value for
	 * @param type The class of return type
	 * @param defaultValue Default value in case element is not present
	 * @param <V> Type of returned value
	 * @return The value of given type represented by the given key or the default value
	 * if the key is not present
	 * @since 5.1
	 */
	@Nullable
	public <V> V get(String key, Class<V> type, @Nullable V defaultValue) {
		Object value = this.map.get(key);
		if (value == null) {
			return defaultValue;
		}
		if (!type.isInstance(value)) {
			throw new ClassCastException("Value for key=[" + key + "] is not of type: [" + type + "], it is [" + "("
					+ value.getClass() + ")" + value + "]");
		}
		return type.cast(value);
	}

	/**
	 * Utility method that attempts to take a value represented by a given key and
	 * validate it as a member of the specified type.
	 * @param key The key to validate a value for
	 * @param type Class against which value should be validated
	 * @return Value typed to the specified <code>Class</code>
	 */
	private <V> V readAndValidate(String key, Class<V> type) {

		Object value = get(key);

		if (!type.isInstance(value)) {
			throw new ClassCastException("Value for key=[" + key + "] is not of type: [" + type + "], it is ["
					+ (value == null ? null : "(" + value.getClass() + ")" + value) + "]");
		}

		return type.cast(value);
	}

	/**
	 * Indicates whether or not the context is empty.
	 * @return True if the context has no entries, false otherwise.
	 * @see java.util.Map#isEmpty()
	 */
	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	/**
	 * Clears the dirty flag.
	 */
	public void clearDirtyFlag() {
		this.dirty = false;
	}

	/**
	 * Returns the entry set containing the contents of this context.
	 * @return An unmodifiable set representing the contents of the context
	 * @see java.util.Map#entrySet()
	 */
	public Set<Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(this.map.entrySet());
	}

	/**
	 * Returns the internal map as read-only.
	 * @return An unmodifiable map containing all contents.
	 * @see java.util.Map
	 * @since 5.1
	 */
	public Map<String, Object> toMap() {
		return Collections.unmodifiableMap(this.map);
	}

	/**
	 * Indicates whether or not a key is represented in this context.
	 * @param key Key to check existence for
	 * @return True if key is represented in context, false otherwise
	 * @see java.util.Map#containsKey(Object)
	 */
	public boolean containsKey(String key) {
		return this.map.containsKey(key);
	}

	/**
	 * Removes the mapping for a key from this context if it is present.
	 * @param key {@link String} that identifies the entry to be removed from the context.
	 * @return the value that was removed from the context.
	 *
	 * @see java.util.Map#remove(Object)
	 */
	@Nullable
	public Object remove(String key) {
		return this.map.remove(key);
	}

	/**
	 * Indicates whether or not a value is represented in this context.
	 * @param value Value to check existence for
	 * @return True if value is represented in context, false otherwise
	 * @see java.util.Map#containsValue(Object)
	 */
	public boolean containsValue(Object value) {
		return this.map.containsValue(value);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExecutionContext rhs)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		return this.entrySet().equals(rhs.entrySet());
	}

	@Override
	public int hashCode() {
		return this.map.hashCode();
	}

	@Override
	public String toString() {
		return this.map.toString();
	}

	/**
	 * Returns number of entries in the context
	 * @return Number of entries in the context
	 * @see java.util.Map#size()
	 */
	public int size() {
		return this.map.size();
	}

}
