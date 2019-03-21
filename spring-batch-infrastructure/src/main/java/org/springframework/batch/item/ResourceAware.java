/*
 * Copyright 2012 the original author or authors.
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
package org.springframework.batch.item;

import org.springframework.core.io.Resource;
import org.springframework.batch.item.file.MultiResourceItemReader;

/**
 * Marker interface indicating that an item should have the Spring {@link Resource} in which it was read from, set on it.
 * The canonical example is within {@link MultiResourceItemReader}, which will set the current resource on any items
 * that implement this interface.
 *
 * @author Lucas Ward
 */
public interface ResourceAware {

    void setResource(Resource resource);
}
