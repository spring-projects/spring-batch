/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.batch.item.file.mapping;

import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.validation.BindException;

/**
 * A basic array mapper, returning the values backing a fieldset.
 * Useful for reading the Strings resulting from the line tokenizer without having to
 * deal with a {@link FieldSet} object.  
 * 
 * @author Costin Leau
 */
public class ArrayFieldSetMapper implements FieldSetMapper<String[]> {

	public String[] mapFieldSet(FieldSet fieldSet) throws BindException {
		return fieldSet.getValues();
	}
}
