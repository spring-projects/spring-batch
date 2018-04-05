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
 * Adapter for long Values
 * 
 * @author Thomas Nill
 * @since 4.0.1
 *
 */
public class LongAdapter extends XmlAdapter<String, Long> {

	@Override
	public Long unmarshal(String value) throws ParseException {
		Assert.notNull(value, "Parameter should not be null");
		return new Long(value.trim());
	}

	@Override
	public String marshal(Long value) {
		Assert.notNull(value, "Parameter should not be null");
		return value.toString();
	}

}
