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

package org.springframework.batch.repeat.context;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;

/**
 * An {@link AttributeAccessor} that synchronizes on a mutex (not this) before
 * modifying or accessing the underlying attributes.
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

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.AttributeAccessor#attributeNames()
	 */
	public String[] attributeNames() {
		synchronized (support) {
			return support.attributeNames();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		AttributeAccessorSupport that;
		if (other instanceof SynchronizedAttributeAccessor) {
			that = ((SynchronizedAttributeAccessor) other).support;
		}
		else if (other instanceof AttributeAccessorSupport) {
			that = (AttributeAccessorSupport) other;
		}
		else {
			return false;
		}
		synchronized (support) {
			return support.equals(that);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.AttributeAccessor#getAttribute(java.lang.String)
	 */
	public Object getAttribute(String name) {
		synchronized (support) {
			return support.getAttribute(name);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.AttributeAccessor#hasAttribute(java.lang.String)
	 */
	public boolean hasAttribute(String name) {
		synchronized (support) {
			return support.hasAttribute(name);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return support.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.AttributeAccessor#removeAttribute(java.lang.String)
	 */
	public Object removeAttribute(String name) {
		synchronized (support) {
			return support.removeAttribute(name);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.core.AttributeAccessor#setAttribute(java.lang.String,
	 * java.lang.Object)
	 */
	public void setAttribute(String name, Object value) {
		synchronized (support) {
			support.setAttribute(name, value);
		}
	}

	/**
	 * Additional support for atomic put if absent.
	 * @param name the key for the attribute name
	 * @param value the value of the attribute
	 * @return null if the attribute was not already set, the existing value
	 * otherwise.
	 */
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

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer("SynchronizedAttributeAccessor: [");
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
