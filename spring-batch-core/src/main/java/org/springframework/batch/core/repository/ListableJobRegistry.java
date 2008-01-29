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
package org.springframework.batch.core.repository;

import java.util.Collection;

import org.springframework.batch.core.domain.Job;

/**
 * A listable extension of {@link JobRegistry}.
 * 
 * @author Dave Syer
 * 
 */
public interface ListableJobRegistry extends JobRegistry {

	/**
	 * Provides the currently registered configurations. The return value is
	 * unmodifiable and disconnected from the underlying registry storage.
	 * 
	 * @return a collection of {@link Job} instances. Empty if none
	 * are registered.
	 */
	Collection getJobConfigurations();
}
