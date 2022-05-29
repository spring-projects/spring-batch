/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.item.ItemProcessor;

/**
 * Marks a method to be called after an item is passed to an {@link ItemProcessor}.
 * {@code item} is the input item. {@code result} is the processed item. {@code result}
 * can be null if the {@code item} is filtered.<br>
 * <br>
 * Expected signature: void afterProcess(T item, S result)
 *
 * @author Lucas Ward, Jay Bryant
 * @since 2.0
 * @see ItemProcessListener
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface AfterProcess {

}
