/*
 * Copyright 2020-2021 the original author or authors.
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
package org.springframework.batch.core.test.repository;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LicenseAcceptance;

import com.github.dockerjava.api.model.Ulimit;

/**
 * @author Jonathan Bregler
 */
public class HANAContainer<SELF extends HANAContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

	private static final Integer PORT = 39041;

	private static final String SYSTEM_USER = "SYSTEM";
	private static final String SYSTEM_USER_PASSWORD = "HXEHana1";

	public HANAContainer(DockerImageName image) {

		super( image );

		addExposedPorts( 39013, 39017, 39041, 39042, 39043, 39044, 39045, 1128, 1129, 59013, 59014 );

		// create ulimits
		Ulimit[] ulimits = new Ulimit[]{ new Ulimit( "nofile", 1048576L, 1048576L ) };

		// create sysctls Map.
		Map<String, String> sysctls = new HashMap<String, String>();

		sysctls.put( "kernel.shmmax", "1073741824" );
		sysctls.put( "net.ipv4.ip_local_port_range", "40000 60999" );

		// Apply mounts, ulimits and sysctls.
		this.withCreateContainerCmdModifier( it -> it.getHostConfig().withUlimits( ulimits ).withSysctls( sysctls ) );

		// Arguments for Image.
		this.withCommand( "--master-password " + SYSTEM_USER_PASSWORD + " --agree-to-sap-license" );

		// Determine if container is ready.
		this.waitStrategy = new LogMessageWaitStrategy().withRegEx( ".*Startup finished!*\\s" ).withTimes( 1 )
				.withStartupTimeout( Duration.of( 600, ChronoUnit.SECONDS ) );
	}

	@Override
	protected void configure() {
		/*
		 * Enforce that the license is accepted - do not remove. License available at:
		 * https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and- exhibit.pdf
		 */

		// If license was not accepted programmatically, check if it was accepted via
		// resource file
		if ( !getEnvMap().containsKey( "AGREE_TO_SAP_LICENSE" ) ) {
			LicenseAcceptance.assertLicenseAccepted( this.getDockerImageName() );
			acceptLicense();
		}
	}

	/**
	 * Accepts the license for the SAP HANA Express container by setting the AGREE_TO_SAP_LICENSE=Y Calling this method
	 * will automatically accept the license at:
	 * https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf
	 * 
	 * @return The container itself with an environment variable accepting the SAP HANA Express license
	 */
	public SELF acceptLicense() {
		addEnv( "AGREE_TO_SAP_LICENSE", "Y" );
		return self();
	}

	@Override
	protected Set<Integer> getLivenessCheckPorts() {
		return new HashSet<>( Arrays.asList( new Integer[]{ getMappedPort( PORT ) } ) );
	}

	@Override
	protected void waitUntilContainerStarted() {
		getWaitStrategy().waitUntilReady( this );
	}

	@Override
	public String getDriverClassName() {
		return "com.sap.db.jdbc.Driver";
	}

	@Override
	public String getUsername() {
		return SYSTEM_USER;
	}

	@Override
	public String getPassword() {
		return SYSTEM_USER_PASSWORD;
	}

	@Override
	public String getTestQueryString() {
		return "SELECT 1 FROM SYS.DUMMY";
	}

	@Override
	public String getJdbcUrl() {
		return "jdbc:sap://" + getContainerIpAddress() + ":" + getMappedPort( PORT ) + "/";
	}
}
