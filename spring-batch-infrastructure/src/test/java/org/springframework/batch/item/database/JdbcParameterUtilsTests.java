/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.batch.item.database;

import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.batch.item.database.JdbcParameterUtils;

import java.util.List;
import java.util.ArrayList;

/**
 * @author Thomas Risberg
 */
public class JdbcParameterUtilsTests {

	@Test
	public void testCountParameterPlaceholders() {
		assertEquals(0, JdbcParameterUtils.countParameterPlaceholders(null, null));
		assertEquals(0, JdbcParameterUtils.countParameterPlaceholders("", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("?", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big\" ? 'bad wolf'", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big ?? bad wolf", null));
		assertEquals(3, JdbcParameterUtils.countParameterPlaceholders("The big ? ? bad ? wolf", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big?\" 'ba''ad?' ? wolf", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders(":parameter", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big\" :parameter 'bad wolf'", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The big :parameter :parameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big :parameter :newpar :parameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big :parameter, :newpar, :parameter bad wolf", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big:\" 'ba''ad:p' :parameter wolf", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("&parameter", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big\" &parameter 'bad wolf'", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The big &parameter &parameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big &parameter &newparameter &parameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big &parameter, &newparameter, &parameter bad wolf", null));
		assertEquals(1, JdbcParameterUtils.countParameterPlaceholders("The \"big &x  \" 'ba''ad&p' &parameter wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big :parameter, &newparameter, &parameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big :parameter, &sameparameter, &sameparameter bad wolf", null));
		assertEquals(2, JdbcParameterUtils.countParameterPlaceholders("The big :parameter, :sameparameter, :sameparameter bad wolf", null));
		assertEquals(0, JdbcParameterUtils.countParameterPlaceholders("xxx & yyy", null));
		List<String> l = new ArrayList<String>();
		assertEquals(3, JdbcParameterUtils.countParameterPlaceholders("select :par1, :par2 :par3", l));
		assertEquals(3, l.size());
	}

}
