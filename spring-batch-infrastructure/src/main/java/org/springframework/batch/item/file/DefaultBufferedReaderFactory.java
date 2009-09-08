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
package org.springframework.batch.item.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.springframework.core.io.Resource;

/**
 * @author Dave Syer
 *
 * @since 2.1
 */
public class DefaultBufferedReaderFactory implements BufferedReaderFactory {

	public BufferedReader create(Resource resource, String encoding) throws UnsupportedEncodingException, IOException {
		return new BufferedReader(new InputStreamReader(resource.getInputStream(), encoding));
	}
	
}
