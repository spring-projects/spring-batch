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

package org.springframework.batch.core;

import java.io.Serializable;

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

	private Long id;

	private volatile Integer version;

	/**
	 * Default constructor for {@link Entity}.
	 * <p>
	 * The ID defaults to zero.
	 */
	public Entity() {
		super();
	}

	/**
	 * The constructor for the {@link Entity} where the ID is established.
	 * @param id The ID for the entity.
	 */
	public Entity(Long id) {
		super();

		// Commented out because StepExecutions are still created in a disconnected
		// manner. The Repository should create them, then this can be uncommented.
		// Assert.notNull(id, "Entity id must not be null.");
		this.id = id;
	}

	/**
	 * @return The ID associated with the {@link Entity}.
	 */
	public Long getId() {
		return id;
	}

	/**
	 * @param id The ID for the {@link Entity}.
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the version.
	 */
	public Integer getVersion() {
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

	/**
	 * Attempt to establish identity based on {@code id} if both exist. If either
	 * {@code id} does not exist, use {@code Object.equals()}.
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (other == null) {
			return false;
		}
		if (!(other instanceof Entity entity)) {
			return false;
		}
		if (id == null || entity.getId() == null) {
			return false;
		}
		return id.equals(entity.getId());
	}

	/**
	 * Use {@code id}, if it exists, to establish a hash code. Otherwise fall back to
	 * {@code Object.hashCode()}. It is based on the same information as {@code equals},
	 * so, if that changes, this will. Note that this follows the contract of
	 * {@code Object.hashCode()} but will cause problems for anyone adding an unsaved
	 * {@link Entity} to a {@code Set} because {@code Set.contains()} almost certainly
	 * returns false for the {@link Entity} after it is saved. Spring Batch does not store
	 * any of its entities in sets as a matter of course, so this is internally
	 * consistent. Clients should not be exposed to unsaved entities.
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (id == null) {
			return System.identityHashCode(this);
		}
		return 39 + 87 * id.hashCode();
	}

}
