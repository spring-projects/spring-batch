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

package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class MultilineOrderJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private static final String EXPECTED_OUTPUT = 
	"BEGIN_ORDER:13100345  2007/02/15                    "+
	"CUSTOMER:20014539  Peter               Smith     "+
	"ADDRESS:Oak Street 31/A     Small Town00235     "+
	"BILLING:VISA      VISA-12345678903    "+
	"ITEM:104439104137.49     "+
	"ITEM:2134776319221.99    "+
	"END_ORDER:              267.34"+
	"BEGIN_ORDER:13100346  2007/02/15                    "+
	"CUSTOMER:72155919                                "+
	"ADDRESS:St. Andrews Road 31 London    55342     "+
	"BILLING:AMEX      AMEX-72345678903    "+
	"ITEM:10443191011070.50   "+
	"ITEM:213472721921.79     "+
	"ITEM:104433930179.95     "+
	"ITEM:213474731955.29     "+
	"ITEM:1044359501339.99    "+
	"END_ORDER:            14043.74";

	
	private Resource fileOutputLocator = new FileSystemResource("target/test-outputs/20070122.teststream.multilineOrderStep.TEMP.txt");
	
	/**
	 * Read the output file and compare it with expected string
	 * @throws IOException 
	 */
	protected void validatePostConditions() throws Exception {
		assertEquals(EXPECTED_OUTPUT, StringUtils.replace(IOUtils.toString(fileOutputLocator.getInputStream()), System.getProperty("line.separator"), ""));
	}

}
