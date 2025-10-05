/*
 * Copyright 2023-2025 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import com.github.dockerjava.api.model.Ulimit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.sap.db.jdbcext.HanaDataSource;
import org.testcontainers.utility.LicenseAcceptance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The official Docker image for SAP HANA is not publicly available. SAP HANA support is
 * tested manually. See <a href=
 * "https://hub.docker.com/_/sap-hana-express-edition/plans/f2dc436a-d851-4c22-a2ba-9de07db7a9ac">...</a>
 * FTR, from the previous link: "This installation does not support Docker for Windows or
 * Docker for Mac."
 *
 * @author Jonathan Bregler
 * @author Mahmoud Ben Hassine
 * @author Sukanth Gunda
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringJUnitConfig
@Disabled("Official Docker image for SAP HANA not publicly available and works only on Linux")
class HANAJobRepositoryIntegrationTests {

	private static final DockerImageName HANA_IMAGE = DockerImageName
		.parse("store/saplabs/hanaexpress:2.00.057.00.20211207.1");

	@Container
	public static HANAContainer<?> hana = new HANAContainer<>(HANA_IMAGE).acceptLicense();

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private Job job;

	@BeforeEach
	void setUp() {
		ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
		databasePopulator.addScript(new ClassPathResource("/org/springframework/batch/core/schema-hana.sql"));
		databasePopulator.execute(this.dataSource);
	}

	@Test
	void testJobExecution() throws Exception {
		// given
		JobParameters jobParameters = new JobParametersBuilder().toJobParameters();

		// when
		JobExecution jobExecution = this.jobOperator.start(this.job, jobParameters);

		// then
		assertNotNull(jobExecution);
		assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableJdbcJobRepository
	static class TestConfiguration {

		@Bean
		public DataSource dataSource() throws Exception {
			HanaDataSource dataSource = new HanaDataSource();
			dataSource.setUser(hana.getUsername());
			dataSource.setPassword(hana.getPassword());
			dataSource.setUrl(hana.getJdbcUrl());
			return dataSource;
		}

		@Bean
		public JdbcTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

		@Bean
		public Job job(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
					.build())
				.build();
		}

	}

	/**
	 * @author Jonathan Bregler
	 */
	static class HANAContainer<SELF extends HANAContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

		private static final Integer PORT = 39041;

		private static final String SYSTEM_USER = "SYSTEM";

		private static final String SYSTEM_USER_PASSWORD = "HXEHana1";

		public HANAContainer(DockerImageName image) {

			super(image);

			addExposedPorts(39013, 39017, 39041, 39042, 39043, 39044, 39045, 1128, 1129, 59013, 59014);

			// create ulimits
			Ulimit[] ulimits = new Ulimit[] { new Ulimit("nofile", 1048576L, 1048576L) };

			// create sysctls Map.
			Map<String, String> sysctls = new HashMap<>();

			sysctls.put("kernel.shmmax", "1073741824");
			sysctls.put("net.ipv4.ip_local_port_range", "40000 60999");

			// Apply mounts, ulimits and sysctls.
			this.withCreateContainerCmdModifier(it -> it.getHostConfig().withUlimits(ulimits).withSysctls(sysctls));

			// Arguments for Image.
			this.withCommand("--master-password " + SYSTEM_USER_PASSWORD + " --agree-to-sap-license");

			// Determine if container is ready.
			this.waitStrategy = new LogMessageWaitStrategy().withRegEx(".*Startup finished!*\\s")
				.withTimes(1)
				.withStartupTimeout(Duration.of(600, ChronoUnit.SECONDS));
		}

		@Override
		protected void configure() {
			/*
			 * Enforce that the license is accepted - do not remove. License available at:
			 * https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and
			 * -exhibit.pdf
			 */

			// If license was not accepted programmatically, check if it was accepted via
			// resource file
			if (!getEnvMap().containsKey("AGREE_TO_SAP_LICENSE")) {
				LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
				acceptLicense();
			}
		}

		/**
		 * Accepts the license for the SAP HANA Express container by setting the
		 * AGREE_TO_SAP_LICENSE=Y Calling this method will automatically accept the
		 * license at: <a href=
		 * "https://www.sap.com/docs/download/cmp/2016/06/sap-hana-express-dev-agmt-and-exhibit.pdf">...</a>
		 * @return The container itself with an environment variable accepting the SAP
		 * HANA Express license
		 */
		public SELF acceptLicense() {
			addEnv("AGREE_TO_SAP_LICENSE", "Y");
			return self();
		}

		@Override
		public Set<Integer> getLivenessCheckPortNumbers() {
			return Set.of(getMappedPort(PORT));
		}

		@Override
		protected void waitUntilContainerStarted() {
			getWaitStrategy().waitUntilReady(this);
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
			return "jdbc:sap://" + getHost() + ":" + getMappedPort(PORT) + "/";
		}

	}

}
