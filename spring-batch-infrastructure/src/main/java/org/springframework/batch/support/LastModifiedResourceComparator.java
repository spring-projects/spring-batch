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
package org.springframework.batch.support;

import java.io.IOException;
import java.util.Comparator;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Comparator to sort resources by the file last modified time.
 * 
 * @author Dave Syer
 * 
 */
public class LastModifiedResourceComparator implements Comparator<Resource> {

	/**
	 * Compare the two resources by last modified time, so that a sorted list of
	 * resources will have oldest first.
	 * 
	 * @throws IllegalArgumentException if one of the resources doesn't exist or
	 * its last modified date cannot be determined
	 * 
	 * @see Comparator#compare(Object, Object)
	 */
	public int compare(Resource r1, Resource r2) {
		Assert.isTrue(r1.exists(), "Resource does not exist: " + r1);
		Assert.isTrue(r2.exists(), "Resource does not exist: " + r2);
		try {
			long diff = r1.getFile().lastModified() - r2.getFile().lastModified();
			return diff > 0 ? 1 : diff < 0 ? -1 : 0;
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Resource modification times cannot be determined (unexpected).", e);
		}
	}

}
