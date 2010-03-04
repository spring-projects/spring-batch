/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.math.BigInteger;

import org.junit.Test;

/**
 * Static utility to help with serialization.
 * 
 * @author Dave Syer
 * 
 */
public class SerializationUtilsTests {

	private static BigInteger FOO = new BigInteger(
			"-97029424235490125267223648383278313796609415534328015655051436753861088839708112925637575585166033560096810615697574744209306031461371833798723505120163874786203211176873686513374052845353833564048");

	@Test
	public void testSerializeCycleSunnyDay() throws Exception {
		assertEquals("foo", SerializationUtils.deserialize(SerializationUtils.serialize("foo")));
	}

	@Test(expected = IllegalStateException.class)
	public void testDeserializeUndefined() throws Exception {
		byte[] bytes = FOO.toByteArray();
		Object foo = SerializationUtils.deserialize(bytes);
		assertNotNull(foo);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSerializeNonSerializable() throws Exception {
		SerializationUtils.serialize(new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeserializeNonSerializable() throws Exception {
		SerializationUtils.deserialize("foo".getBytes());
	}

	@Test
	public void testSerializeNull() throws Exception {
		assertNull(SerializationUtils.serialize(null));
	}

	@Test
	public void testDeserializeNull() throws Exception {
		assertNull(SerializationUtils.deserialize(null));
	}

}
