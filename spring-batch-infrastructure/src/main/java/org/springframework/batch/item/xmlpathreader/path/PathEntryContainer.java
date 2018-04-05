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

package org.springframework.batch.item.xmlpathreader.path;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * The PathEntryContainer is a bunch of PathEntry. The searching in the list is not the default search method.
 * @author Thomas Nill
 * @since 4.0.1
 * @see PathEntryContainer
 * @see PathEntry
 *
 * @param <T> the type of the entrys
 */
public class PathEntryContainer<T extends PathEntry> extends ArrayList<T> {
	private static final long serialVersionUID = -6372325656898062269L;

	private static final Logger log = LoggerFactory.getLogger(PathEntryContainer.class);

	/**
	 * Constructor of parent class
	 */
	public PathEntryContainer() {
		super();
	}

	/**
	 * put an entry into the container
	 * 
	 * @param entry the entry with is put into the container
	 */
	public void put(T entry) {
		Assert.notNull(entry, "The entry should not be null");

		log.debug(" add an entry {} of class {} ", entry.getPath(), entry.getClass().getName());
		add(entry);
	}

	/**
	 * get the best matching entry. The best path is a path that is matching and is deep, eg has many elements.
	 * 
	 * @param path the path for the matching
	 * @return the best entry with this matching path
	 */
	public T searchTheBestMatchingEntity(XmlElementPath path) {
		Assert.notNull(path, "The path should not be null");
		log.debug("searchTheBestMatchingEntity for the path {} ", path);
		int bestDepth = 0;
		T bestMatch = null;
		for (T e : this) {
			XmlElementPath p = e.getPath();
			if (path.compare(p) && p.getDepth() > bestDepth) {
				log.debug("better path {} ", p);
				bestMatch = e;
				bestDepth = p.getDepth();
			}
		}
		return bestMatch;
	}

}
