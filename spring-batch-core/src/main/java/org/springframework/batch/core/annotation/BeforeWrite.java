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

package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.item.ItemWriter;

/**
 * Marks a method to be called before an item is passed to an {@link ItemWriter} <br>
 * <br>
 * Expected signature: void beforeWrite({@link List}&lt;? extends S&gt; items)
 * 
 * @author Lucas Ward
 * @since 2.0
 * @see ItemWriteListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BeforeWrite {

}
