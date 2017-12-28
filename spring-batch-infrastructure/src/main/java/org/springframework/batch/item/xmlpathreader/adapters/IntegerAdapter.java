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
 * Adapter for integer Values
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */

public class IntegerAdapter extends XmlAdapter<String, Integer> {

	@Override
	public Integer unmarshal(String value) throws ParseException {
		Assert.notNull(value, "Parameter should not be null");
		return new Integer(value.trim());
	}

	@Override
	public String marshal(Integer value) {
		Assert.notNull(value, "Parameter should not be null");
		return value.toString();
	}

}
