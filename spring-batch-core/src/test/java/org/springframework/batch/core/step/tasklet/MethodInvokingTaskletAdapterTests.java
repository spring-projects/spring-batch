/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.step.tasklet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * @author Mahmoud Ben Hassine
 */
class MethodInvokingTaskletAdapterTests {

	private StepContribution stepContribution;

	private ChunkContext chunkContext;

	private TestTasklet tasklet;

	private MethodInvokingTaskletAdapter adapter;

	@BeforeEach
	void setUp() {
		stepContribution = new StepContribution(mock());
		chunkContext = mock();
		tasklet = new TestTasklet();
		adapter = new MethodInvokingTaskletAdapter();
		adapter.setTargetObject(tasklet);
	}

	@Test
	void testExactlySameSignature() throws Exception {
		adapter.setTargetMethod("execute");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getStepContribution(), stepContribution);
		assertEquals(tasklet.getChunkContext(), chunkContext);
	}

	@Test
	void testSameSignatureWithDifferentMethodName() throws Exception {
		adapter.setTargetMethod("execute1");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getStepContribution(), stepContribution);
		assertEquals(tasklet.getChunkContext(), chunkContext);
	}

	@Test
	void testDifferentParametersOrder() throws Exception {
		adapter.setTargetMethod("execute2");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getStepContribution(), stepContribution);
		assertEquals(tasklet.getChunkContext(), chunkContext);
	}

	@Test
	void testArgumentSubsetWithOnlyChunkContext() throws Exception {
		adapter.setTargetMethod("execute3");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getChunkContext(), chunkContext);
	}

	@Test
	void testArgumentSubsetWithOnlyStepContribution() throws Exception {
		adapter.setTargetMethod("execute4");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getStepContribution(), stepContribution);
	}

	@Test
	void testArgumentSubsetWithoutArguments() throws Exception {
		adapter.setTargetMethod("execute5");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	void testCompatibleReturnTypeWhenBoolean() throws Exception {
		adapter.setTargetMethod("execute6");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	void testCompatibleReturnTypeWhenVoid() throws Exception {
		adapter.setTargetMethod("execute7");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
	}

	@Test
	void testArgumentSubsetWithOnlyStepContributionAndCompatibleReturnTypeBoolean() throws Exception {
		adapter.setTargetMethod("execute8");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getStepContribution(), stepContribution);
	}

	@Test
	void testArgumentSubsetWithOnlyChunkContextAndCompatibleReturnTypeVoid() throws Exception {
		adapter.setTargetMethod("execute9");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(tasklet.getChunkContext(), chunkContext);
	}

	@Test
	void testIncorrectSignatureWithExtraParameter() {
		adapter.setTargetMethod("execute10");
		assertThrows(IllegalArgumentException.class, () -> adapter.execute(stepContribution, chunkContext));
	}

	@Test
	void testExitStatusReturnType() throws Exception {
		adapter.setTargetMethod("execute11");
		adapter.execute(stepContribution, chunkContext);
		assertEquals(new ExitStatus("DONE"), stepContribution.getExitStatus());
	}

	@Test
	void testNonExitStatusReturnType() throws Exception {
		adapter.setTargetMethod("execute12");
		RepeatStatus repeatStatus = adapter.execute(stepContribution, chunkContext);
		assertEquals(RepeatStatus.FINISHED, repeatStatus);
		assertEquals(ExitStatus.COMPLETED, stepContribution.getExitStatus());
	}

	/*
	 * <xsd:attribute name="method" type="xsd:string" use="optional"> <xsd:annotation>
	 * <xsd:documentation> If the tasklet is specified as a bean definition, then a method
	 * can be specified and a POJO will be adapted to the Tasklet interface. The method
	 * suggested should have the same arguments as Tasklet.execute (or a subset), and have
	 * a compatible return type (boolean, void or RepeatStatus). </xsd:documentation>
	 * </xsd:annotation> </xsd:attribute>
	 */
	public static class TestTasklet {

		private StepContribution stepContribution;

		private ChunkContext chunkContext;

		/* exactly same signature */
		public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return RepeatStatus.FINISHED;
		}

		/* same signature, different method name */
		public RepeatStatus execute1(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return RepeatStatus.FINISHED;
		}

		/* different parameters order */
		public RepeatStatus execute2(ChunkContext chunkContext, StepContribution stepContribution) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return RepeatStatus.FINISHED;
		}

		/* subset of arguments: only chunk context */
		public RepeatStatus execute3(ChunkContext chunkContext) throws Exception {
			this.chunkContext = chunkContext;
			return RepeatStatus.FINISHED;
		}

		/* subset of arguments: only step contribution */
		public RepeatStatus execute4(StepContribution stepContribution) throws Exception {
			this.stepContribution = stepContribution;
			return RepeatStatus.FINISHED;
		}

		/* subset of arguments: no arguments */
		public RepeatStatus execute5() throws Exception {
			return RepeatStatus.FINISHED;
		}

		/* compatible return type: boolean */
		public boolean execute6(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return true;
		}

		/* compatible return type: void */
		public void execute7(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
		}

		/*
		 * subset of arguments (only step contribution) and compatible return type
		 * (boolean)
		 */
		public boolean execute8(StepContribution stepContribution) throws Exception {
			this.stepContribution = stepContribution;
			return true;
		}

		/* subset of arguments (only chunk context) and compatible return type (void) */
		public void execute9(ChunkContext chunkContext) throws Exception {
			this.chunkContext = chunkContext;
		}

		/*
		 * Incorrect signature: extra parameter (ie a superset not a subset as specified)
		 */
		public RepeatStatus execute10(StepContribution stepContribution, ChunkContext chunkContext, String string)
				throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return RepeatStatus.FINISHED;
		}

		/* ExitStatus return type : should be returned as is */
		public ExitStatus execute11(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return new ExitStatus("DONE");
		}

		/* Non ExitStatus return type : should return ExitStatus.COMPLETED */
		public String execute12(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
			this.stepContribution = stepContribution;
			this.chunkContext = chunkContext;
			return "DONE";
		}

		public StepContribution getStepContribution() {
			return stepContribution;
		}

		public ChunkContext getChunkContext() {
			return chunkContext;
		}

	}

}
