/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.support;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

public class IntArrayPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		String[] strs = StringUtils.commaDelimitedListToStringArray(text);
		int[] value = new int[strs.length];
		for (int i = 0; i < value.length; i++) {
			value[i] = Integer.valueOf(strs[i].trim()).intValue();
		}
		setValue(value);
	}

}
