/*
 * Copyright 2026 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.infrastructure.item.ldif.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * @author Banseok Kim
 */
public abstract class LdifTestFixtures {

	protected static final String LDIF = """
			dn: cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			cn: Barbara Jensen
			sn: Jensen

			dn: cn=Bjorn Jensen,ou=Accounting,dc=airius,dc=com
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			cn: Bjorn Jensen
			sn: Jensen

			dn: cn=Gern Jensen,ou=Product Testing,dc=airius,dc=com
			objectclass: top
			objectclass: person
			objectclass: organizationalPerson
			cn: Gern Jensen
			sn: Jensen
			""";

	protected final Resource ldifResource = new ByteArrayResource(LDIF.getBytes(StandardCharsets.UTF_8), "test.ldif");

	protected List<String> expectedDns() {
		return List.of("cn=Barbara Jensen,ou=Product Development,dc=airius,dc=com",
				"cn=Bjorn Jensen,ou=Accounting,dc=airius,dc=com", "cn=Gern Jensen,ou=Product Testing,dc=airius,dc=com");
	}

	protected Resource missingResource() throws IOException {
		Path missing = Files.createTempFile("missing-", ".ldif");
		Files.delete(missing);
		return new FileSystemResource(missing.toFile());
	}

}
