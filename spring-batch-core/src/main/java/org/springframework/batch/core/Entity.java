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

package org.springframework.batch.core;

import java.io.Serializable;

import org.springframework.util.ClassUtils;

/**
 * Batch Domain Entity class. Any class that should be uniquely identifiable
 * from another should subclass from Entity. More information on this pattern
 * and the difference between Entities and Value Objects can be found in Domain
 * Driven Design by Eric Evans.
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class Entity implements Serializable {

	private Long id;

	private volatile Integer version;

	public Entity() {
		super();
	}

	public Entity(Long id) {
		super();
		
		//Commented out because StepExecutions are still created in a disconnected
		//manner.  The Repository should create them, then this can be uncommented.
		//Assert.notNull(id, "Entity id must not be null.");
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the version
	 */
	public Integer getVersion() {
		return version;
	}
	
	/**
	 * Public setter for the version needed only by repository methods.
	 * @param version the version to set
	 */
	public void setVersion(Integer version) {
		this.version = version;
	}

	/**
	 * Increment the version number
	 */
	public void incrementVersion() {
		if (version == null) {
			version = 0;
		} else {
			version = version + 1;
		}
	}

	@Override
	public String toString() {
		return String.format("%s: id=%d, version=%d", ClassUtils.getShortName(getClass()), id, version);
	}

	/**
	 * Attempt to establish identity based on id if both exist. If either id
	 * does not exist use Object.equals().
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
		if (!(other instanceof Entity)) {
			return false;
		}
		Entity entity = (Entity) other;
		if (id == null || entity.getId() == null) {
			return false;
		}
		return id.equals(entity.getId());
	}

	/**
	 * Use ID if it exists to establish hash code, otherwise fall back to
	 * Object.hashCode(). Based on the same information as equals, so if that
	 * changes, this will. N.B. this follows the contract of Object.hashCode(),
	 * but will cause problems for anyone adding an unsaved {@link Entity} to a
	 * Set because Set.contains() will almost certainly return false for the
	 * {@link Entity} after it is saved. Spring Batch does not store any of its
	 * entities in Sets as a matter of course, so internally this is consistent.
	 * Clients should not be exposed to unsaved entities.
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		if (id == null) {
			return super.hashCode();
		}
		return 39 + 87 * id.hashCode();
	}

}
