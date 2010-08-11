/*
 * Copyright 2009-2010 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.jms.JmsHeaders;

/**
 * @author Dave Syer
 *
 */
public class JmsRedeliveredExtractor {
	
	private static final Log logger = LogFactory.getLog(JmsRedeliveredExtractor.class);
	
	public ChunkResponse extract(ChunkResponse input, @Header(JmsHeaders.REDELIVERED) boolean redelivered) {
		logger.debug("Extracted redelivered flag for response, value="+redelivered);
		return new ChunkResponse(input, redelivered);
	}

}
