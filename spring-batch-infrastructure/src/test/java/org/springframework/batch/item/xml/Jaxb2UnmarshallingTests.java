/*
 * Copyright 2010-2014 the original author or authors.
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
package org.springframework.batch.item.xml;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class Jaxb2UnmarshallingTests extends AbstractStaxEventReaderItemReaderTests {

	@Override
	protected Unmarshaller getUnmarshaller() throws Exception {
		reader.setFragmentRootElementName("trade");

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class<?>[] { Trade.class });
		// marshaller.setSchema(new ClassPathResource("trade.xsd", Trade.class));
		marshaller.afterPropertiesSet();
		
		return marshaller;
	}

}
