/*
 * Copyright 2010-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.xml;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.transform.stream.StreamResult;

import org.springframework.batch.infrastructure.item.xml.domain.Trade;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

class Jaxb2MarshallingTests extends AbstractStaxEventWriterItemWriterTests {

	@Override
	protected Marshaller getMarshaller() throws Exception {

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class<?>[] { Trade.class });
		marshaller.afterPropertiesSet();

		StringWriter string = new StringWriter();
		marshaller.marshal(new Trade("FOO", 100, BigDecimal.valueOf(10.), "bar"), new StreamResult(string));
		String content = string.toString();
		assertTrue(content.contains("<customer>bar</customer>"), "Wrong content: " + content);
		return marshaller;
	}

}
