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
package org.springframework.batch.core.step.item;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.StepListener;

/**
 * Package private helper for step factory beans.
 * 
 * @author Dave Syer
 * 
 */
abstract class BatchListenerFactoryHelper {

	public static <T> List<T> getListeners(StepListener[] listeners, Class<? super T> cls) {
		List<T> list = new ArrayList<T>();
		for (int i = 0; i < listeners.length; i++) {
			StepListener stepListener = listeners[i];
			if (cls.isAssignableFrom(stepListener.getClass())) {
				@SuppressWarnings("unchecked")
				T listener = (T) stepListener;
				list.add(listener);
			}
		}
		return list;
	}

}
