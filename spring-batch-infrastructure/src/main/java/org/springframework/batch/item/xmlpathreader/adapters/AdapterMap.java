/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.adapters;

import java.util.HashMap;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.util.Assert;

/**
 * a bundle of different adapters that convert String to Object, you can add other adapters to this bundle
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class AdapterMap {
	private static final String ADAPTER_SHOULD_NOT_BE_NULL = "The adapter should not be null";

	private static final String CLASS_SHOULD_NOT_BE_NULL = "The class should not be null";

	private static HashMap<Class<?>, XmlAdapter<String, ?>> adapters = null;

	static {
		adapters = new HashMap<>();
		addAdapter(Integer.class, new IntegerAdapter());
		addAdapter(int.class, new IntegerAdapter());
		addAdapter(Long.class, new LongAdapter());
		addAdapter(long.class, new LongAdapter());
		addAdapter(Double.class, new DoubleAdapter());
		addAdapter(double.class, new DoubleAdapter());
		addAdapter(Float.class, new FloatAdapter());
		addAdapter(float.class, new FloatAdapter());
		addAdapter(Boolean.class, new BooleanAdapter());
		addAdapter(boolean.class, new BooleanAdapter());

	}

	private AdapterMap() {
		super();
	}

	/**
	 * add an adapter for a class targetClass
	 * 
	 * @param targetClass the class that is converted
	 * @param adapter the added adapter
	 */
	public static void addAdapter(Class<?> targetClass, XmlAdapter<String, ?> adapter) {
		Assert.notNull(targetClass, CLASS_SHOULD_NOT_BE_NULL);
		Assert.notNull(adapter, ADAPTER_SHOULD_NOT_BE_NULL);
		adapters.put(targetClass, adapter);
	}

	/**
	 * get the adapter for the targetClass
	 * 
	 * @param targetClass the class that is converted
	 * 
	 * @return the adapter for the targetClass
	 */
	public static XmlAdapter<String, ?> getAdapter(Class<?> targetClass) {
		Assert.notNull(targetClass, CLASS_SHOULD_NOT_BE_NULL);
		return adapters.get(targetClass);
	}

	/**
	 * check if an adapter for a class targetClass exists
	 * 
	 * @param targetClass the class that is converted
	 * @return has the targetClass an adapter
	 */
	public static boolean hasAdapterForClass(Class<?> targetClass) {
		Assert.notNull(targetClass, CLASS_SHOULD_NOT_BE_NULL);
		return adapters.containsKey(targetClass);
	}

}
