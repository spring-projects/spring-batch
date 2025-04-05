/*
 * Copyright 2006-2023 the original author or authors.
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

package org.springframework.batch.repeat.context;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;

/**
 * An {@link AttributeAccessor} that synchronizes on a mutex (not this) before modifying
 * or accessing the underlying attributes.
 *
 * @author Dave Syer
 *
 */
public class SynchronizedAttributeAccessor implements AttributeAccessor {

	/**
	 * All methods are delegated to this support object.
	 */
	AttributeAccessorSupport support = new AttributeAccessorSupport() {
		/**
		 * Generated serial UID.
		 */
		private static final long serialVersionUID = -7664290016506582290L;

	};

	@Override
	public String[] attributeNames() {
		synchronized (support) {
			return support.attributeNames();
		}
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		AttributeAccessorSupport that;
		if (other instanceof SynchronizedAttributeAccessor synchronizedAttributeAccessor) {
			that = synchronizedAttributeAccessor.support;
		}
		else if (other instanceof AttributeAccessorSupport attributeAccessorSupport) {
			that = attributeAccessorSupport;
		}
		else {
			return false;
		}
		synchronized (support) {
			return support.equals(that);
		}
	}

	@Override
	public Object getAttribute(String name) {
		synchronized (support) {
			return support.getAttribute(name);
		}
	}

	@Override
	public boolean hasAttribute(String name) {
		synchronized (support) {
			return support.hasAttribute(name);
		}
	}

	@Override
	public int hashCode() {
		return support.hashCode();
	}

	@Override
	public Object removeAttribute(String name) {
		synchronized (support) {
			return support.removeAttribute(name);
		}
	}

	@Override
	public void setAttribute(String name, Object value) {
		synchronized (support) {
			support.setAttribute(name, value);
		}
	}

	/**
	 * Additional support for atomic put if absent.
	 * @param name the key for the attribute name
	 * @param value the value of the attribute
	 * @return null if the attribute was not already set, the existing value otherwise.
	 */
	@Nullable
	public Object setAttributeIfAbsent(String name, Object value) {
		synchronized (support) {
			Object old = getAttribute(name);
			if (old != null) {
				return old;
			}
			setAttribute(name, value);
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder("SynchronizedAttributeAccessor: [");
		synchronized (support) {
			String[] names = attributeNames();
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				buffer.append(names[i]).append("=").append(getAttribute(name));
				if (i < names.length - 1) {
					buffer.append(", ");
				}
			}
			buffer.append("]");
			return buffer.toString();
		}
	}

}
