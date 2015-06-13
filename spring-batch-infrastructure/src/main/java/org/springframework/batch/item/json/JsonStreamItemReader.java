/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.batch.item.json;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.json.Json;
import javax.json.stream.JsonParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.ojm.Unmarshaller;
import org.springframework.util.Assert;

public class JsonStreamItemReader<T> extends AbstractItemCountingItemStreamItemReader<T> 
	implements ResourceAwareItemReaderItemStream<T>, InitializingBean {

	private static final Log logger = LogFactory.getLog(JsonStreamItemReader.class);
	private boolean strict = true;
	private boolean noInput;

	private Resource resource;
	private InputStream inputStream;
	private JsonParser jsonParser;

	private String keyName;
	
	private Unmarshaller<T> unmarshaller;
	private Class<T> targetClass;

	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	public void setUnmarshaller(Unmarshaller<T> unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	public void setTargetClass(Class<T> targetClass) {
		this.targetClass = targetClass;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(unmarshaller, "The Unmarshaller must not be null.");
		Assert.notNull(keyName, "The keyName may be empty, but it cannot be null.");
	}

	@Override
	public void setResource(Resource resource) {
		this.resource = resource;		
	}

	@Override
	protected T doRead() throws Exception {
		if (noInput) {
			return null;
		}

		try {
			int nestingLevel = 0;
			StringBuilder json = new StringBuilder();
			JsonParser.Event nextEvent;

			while (true) {
				nextEvent = jsonParser.next();
				switch (nextEvent) {
				/* If we hit the end of an array/object, there will always be an 
				 * excess comma from the other switch statement.  Remove it before 
				 * closing the array/object.
				 */
				case END_ARRAY: 
					if (0 == nestingLevel) {
						noInput = true;
						return null;
					} else {
						json = json.deleteCharAt(json.length() - 1).append(']');
						nestingLevel--;
						break;						
					}
				case END_OBJECT: 
					// empty object corner case
					if (1 < json.length()) {
						json = json.deleteCharAt(json.length() - 1);
					}
					json = json.append('}');
					/* we need to track the nesting level.  only the end of the root 
					 * object should the json be unmarshalled.  otherwise, we should continue 
					 * to build the object.
					 */
					if (1 == nestingLevel) {
						T item = unmarshaller.unmarshal(new ByteArrayInputStream(json.toString().getBytes()), targetClass);
						return item;
					} else {
						nestingLevel--;
						break;
					}
				case KEY_NAME: 
					json = json.append('"').append(jsonParser.getString()).append('"').append(":");
					break;
				case START_ARRAY: 
					json = json.append('[');
					nestingLevel++;
					break;
				case START_OBJECT: 
					json = json.append('{');
					nestingLevel++;
					break;
				case VALUE_FALSE: 
					json = json.append("false");
					break;
				case VALUE_NULL: 
					json = json.append("null");
					break;
				case VALUE_TRUE: 
					json = json.append("true");
					break;
				case VALUE_STRING: 
					json = json.append('"').append(jsonParser.getString().replaceAll("\"", "\\\\\"")).append('"');
					break;
				case VALUE_NUMBER: 
					if (jsonParser.isIntegralNumber()) {
						json = json.append(jsonParser.getLong());
					} else {
						json = json.append(jsonParser.getBigDecimal());
					}
					break;
				}

				/* For any value inside an object/array, we need to add a comma.
				 * This will create a problem with the last element, but we can remove 
				 * the extra comma at that time. 
				 */
				switch (nextEvent) {
				case END_ARRAY:
				case END_OBJECT:
				case VALUE_FALSE: 
				case VALUE_NULL: 
				case VALUE_TRUE: 
				case VALUE_STRING: 
				case VALUE_NUMBER:
					json = json.append(",");
					break;
				default: 
					break;
				}
			}
		} catch (Exception e) {
			// Prevent caller from retrying indefinitely since this is fatal
			noInput = true;
			throw e;			
		}
	}

	@Override
	protected void doOpen() throws Exception {
		noInput = true;
		if (!resource.exists()) {
			if (strict) {
				throw new IllegalStateException("Input resource must exist (reader is in 'strict' mode)");
			}
			logger.warn("Input resource does not exist " + resource.getDescription());
			return;
		}
		if (!resource.isReadable()) {
			if (strict) {
				throw new IllegalStateException("Input resource must be readable (reader is in 'strict' mode)");
			}
			logger.warn("Input resource is not readable " + resource.getDescription());
			return;
		}

		inputStream = resource.getInputStream();
		jsonParser = Json.createParser(inputStream);
		noInput = false;

		// move the cursor to the start of the input.  ignore everything before that.
		while (jsonParser.hasNext()) {
			JsonParser.Event nextEvent = jsonParser.next();
			if (JsonParser.Event.KEY_NAME == nextEvent && jsonParser.getString().equals(keyName)) {
				// A JSON array is expected, otherwise it's a format error.
				nextEvent = jsonParser.next();
				Assert.isTrue(JsonParser.Event.START_ARRAY == nextEvent
					, "Value at element with key '" + keyName + "' must be an array."
				);
				return;
			}
		}
	}

	@Override
	protected void doClose() throws Exception {
		try {
			if (null != jsonParser) {
				jsonParser.close();
			}
			if (null != inputStream) {
				inputStream.close();
			}
		}
		finally {
			jsonParser = null;
			inputStream = null;
		}
	}
}
