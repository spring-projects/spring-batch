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

package org.springframework.batch.core;

import java.io.Serializable;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

/**
 * Batch Domain Entity class. Any class that should be uniquely identifiable from another
 * should subclass from Entity. See Domain Driven Design, by Eric Evans, for more
 * information on this pattern and the difference between Entities and Value Objects.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class Entity implements Serializable {

	private final long id;

	private @Nullable Integer version;

	/**
	 * The constructor for the {@link Entity} where the ID is established.
	 * @param id The ID for the entity.
	 */
	public Entity(long id) {
		this.id = id;
	}

	/**
	 * @return The ID associated with the {@link Entity}.
	 */
	public long getId() {
		return id;
	}

	/**
	 * @return the version.
	 */
	public @Nullable Integer getVersion() {
		return version;
	}

	/**
	 * Public setter for the version. Needed only by repository methods.
	 * @param version The version to set.
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Increment the version number.
	 */
	public void incrementVersion() {
		if (version == null) {
			version = 0;
		}
		else {
			version = version + 1;
		}
	}

	/**
	 * Creates a string representation of the {@code Entity}, including the {@code id},
	 * {@code version}, and class name.
	 */
	@Override
	public String toString() {
		return String.format("%s: id=%d, version=%d", ClassUtils.getShortName(getClass()), id, version);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Entity entity))
			return false;
		return id == entity.id;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(id);
	}

}
