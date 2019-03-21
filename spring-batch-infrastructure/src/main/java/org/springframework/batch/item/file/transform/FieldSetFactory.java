/*
 * Copyright 2009-2010 the original author or authors.
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
package org.springframework.batch.item.file.transform;

/**
 * Factory interface for creating {@link FieldSet} instances.
 * 
 * @author Dave Syer
 *
 */
public interface FieldSetFactory {
	
	/**
	 * Create a FieldSet with named tokens. The token values can then be
	 * retrieved either by name or by column number.
	 *
	 * @param values the token values
	 * @param names the names of the tokens
	 * @return an instance of {@link FieldSet}.
	 *
	 * @see DefaultFieldSet#readString(String)
	 */
	FieldSet create(String[] values, String[] names);

	/**
	 * Create a FieldSet with anonymous tokens. They can only be retrieved by
	 * column number.
	 *
	 * @param values the token values
	 * @return an instance of {@link FieldSet}.
	 *
	 * @see FieldSet#readString(int)
	 */
	FieldSet create(String[] values);

}
