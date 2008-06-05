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
package org.springframework.batch.item.file.mapping;


/**
 * Pass through {@link FieldSetMapper} useful for passing a {@link FieldSet}
 * back directly rather than a mapped object.
 * 
 * @author Lucas Ward
 * 
 */
public class PassThroughFieldSetMapper implements FieldSetMapper, FieldSetCreator {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.io.file.FieldSetMapper#mapLine(org.springframework.batch.io.file.FieldSet)
	 */
	public Object mapLine(FieldSet fs, int rownum) {
		return fs;
	}

	/**
	 * If the input is a {@link FieldSet} pass it to the caller. Otherwise
	 * convert to a String with toString() and convert it to a single field
	 * {@link FieldSet}.
	 * 
	 * @see org.springframework.batch.item.file.mapping.FieldSetCreator#mapItem(java.lang.Object)
	 */
	public FieldSet mapItem(Object data) {
		if (data instanceof FieldSet) {
			return (FieldSet) data;
		}
		if (!(data instanceof String)) {
			data = "" + data;
		}
		return new DefaultFieldSet(new String[] { (String) data });
	}

}
