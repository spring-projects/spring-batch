/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.batch.item.xmlpathreader.adapters;

import java.text.ParseException;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.springframework.util.Assert;

/**
 * Adapter for boolean values
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class BooleanAdapter extends XmlAdapter<String, Boolean> {

	@Override
	public Boolean unmarshal(String value) throws ParseException {
		Assert.notNull(value, "Parameter should not be null");
		return new Boolean("true yes ok 1 ".indexOf(value.trim().toLowerCase()) >= 0);
	}

	@Override
	public String marshal(Boolean value) {
		Assert.notNull(value, "Parameter should not be null");
		return value.toString();
	}

}
