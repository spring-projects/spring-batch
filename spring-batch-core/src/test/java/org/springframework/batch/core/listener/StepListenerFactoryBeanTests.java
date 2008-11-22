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
package org.springframework.batch.core.listener;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_CHUNK;
import static org.springframework.batch.core.listener.StepListenerMetaData.AFTER_STEP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.util.Assert;

/**
 * @author Lucas Ward
 *
 */
public class StepListenerFactoryBeanTests {

	StepListenerFactoryBean factoryBean;
	TestClass testClass;
	JobExecution jobExecution = new JobExecution(11L); 
	StepExecution stepExecution = new StepExecution("testStep", jobExecution);
	
	@Before
	public void setUp(){
		factoryBean = new StepListenerFactoryBean();
		testClass = new TestClass();
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testStepAndChunk() throws Exception{
		
		factoryBean.setDelegate(testClass);
		Map<StepListenerMetaData, String> metaDataMap = new HashMap<StepListenerMetaData, String>();;
		metaDataMap.put(AFTER_STEP, "destroy");
		metaDataMap.put(AFTER_CHUNK, "afterChunk");
		factoryBean.setMetaDataMap(metaDataMap);
		Object item = new Object();
		List<Object> items = new ArrayList<Object>();
		items.add(item);
		StepListener listener = (StepListener) factoryBean.getObject(); 
		((StepExecutionListener)listener).beforeStep(stepExecution);
		((StepExecutionListener)listener).afterStep(stepExecution);
		((ChunkListener)listener).beforeChunk();
		((ChunkListener)listener).afterChunk();
		((ItemReadListener<Object>)listener).beforeRead();
		((ItemReadListener<Object>)listener).afterRead(item);
		((ItemReadListener)listener).onReadError(new Exception());
		((ItemProcessListener<Object, Object>)listener).beforeProcess(item);
		((ItemProcessListener<Object, Object>)listener).afterProcess(item, item);
		((ItemProcessListener<Object, Object>)listener).onProcessError(item, new Exception());
		((ItemWriteListener<Object>)listener).beforeWrite(items);
		((ItemWriteListener<Object>)listener).afterWrite(items);
		((ItemWriteListener<Object>)listener).onWriteError(new Exception(), items);
		((SkipListener<Object, Object>)listener).onSkipInRead(new Throwable());
		((SkipListener<Object, Object>)listener).onSkipInProcess(item, new Throwable());
		((SkipListener<Object, Object>)listener).onSkipInWrite(item, new Throwable());
		assertTrue(testClass.beforeStepCalled);
		assertTrue(testClass.beforeChunkCalled);
		assertTrue(testClass.afterChunkCalled);
		assertTrue(testClass.beforeReadCalled);
		assertTrue(testClass.afterReadCalled);
		assertTrue(testClass.onReadErrorCalled);
		assertTrue(testClass.beforeProcessCalled);
		assertTrue(testClass.afterProcessCalled);
		assertTrue(testClass.onProcessErrorCalled);
		assertTrue(testClass.beforeWriteCalled);
		assertTrue(testClass.afterWriteCalled);
		assertTrue(testClass.onWriteErrorCalled);
		assertTrue(testClass.onSkipInReadCalled);
		assertTrue(testClass.onSkipInProcessCalled);
		assertTrue(testClass.onSkipInWriteCalled);
	}
	
	@Test
	public void testAllThreeTypes() throws Exception{
		//Test to make sure if someone has annotated a method, implemented the interface, and given a string
		//method name, that all three will be called
		ThreeStepExecutionListener delegate = new ThreeStepExecutionListener();
		factoryBean.setDelegate(delegate);
		Map<StepListenerMetaData, String> metaDataMap = new HashMap<StepListenerMetaData, String>();;
		metaDataMap.put(AFTER_STEP, "destroy");
		factoryBean.setMetaDataMap(metaDataMap);
		StepListener listener = (StepListener) factoryBean.getObject();
		((StepExecutionListener)listener).afterStep(stepExecution);
		assertEquals(3, delegate.callcount);
	}
	
	@Test
	public void testAnnotatingInterfaceResultsInOneCall() throws Exception{
		MultipleAfterStep delegate = new MultipleAfterStep();
		factoryBean.setDelegate(delegate);
		Map<StepListenerMetaData, String> metaDataMap = new HashMap<StepListenerMetaData, String>();;
		metaDataMap.put(AFTER_STEP, "afterStep");
		factoryBean.setMetaDataMap(metaDataMap);
		StepListener listener = (StepListener) factoryBean.getObject();
		((StepExecutionListener)listener).afterStep(stepExecution);
		assertEquals(1, delegate.callcount);
	}
	
	private class MultipleAfterStep implements StepExecutionListener{

		int callcount = 0;
		
		@AfterStep
		public ExitStatus afterStep(StepExecution stepExecution) {
			Assert.notNull(stepExecution);
			callcount++;
			return null;
		}

		public void beforeStep(StepExecution stepExecution) {
			callcount++;
		}
		
	
	}
	
	private class ThreeStepExecutionListener implements StepExecutionListener{

		int callcount = 0;
		
		public ExitStatus afterStep(StepExecution stepExecution) {
			Assert.notNull(stepExecution);
			callcount++;
			return null;
		}
		
		public void beforeStep(StepExecution stepExecution) {
			callcount++;
		}
		
		public void destroy(){
			callcount++;
		}
		
		@AfterStep
		public void after(){
			callcount++;
		}

	}
	
	private class TestClass implements SkipListener<Object, Object>{

		boolean beforeStepCalled = false;
		boolean afterStepCalled = false;
		boolean beforeChunkCalled = false;
		boolean afterChunkCalled = false;
		boolean beforeReadCalled = false;
		boolean afterReadCalled = false;
		boolean onReadErrorCalled = false;
		boolean beforeProcessCalled = false;
		boolean afterProcessCalled = false;
		boolean onProcessErrorCalled = false;
		boolean beforeWriteCalled = false;
		boolean afterWriteCalled = false;
		boolean onWriteErrorCalled = false;
		boolean onSkipInReadCalled = false;
		boolean onSkipInProcessCalled = false;
		boolean onSkipInWriteCalled = false;
		
		@BeforeStep
		public void initStep(){
			beforeStepCalled = true;
		}
		
		public void destroy(){
			afterStepCalled = true;
		}
		
		@BeforeChunk
		public void before(){
			beforeChunkCalled = true;
		}
		
		public void afterChunk(){
			afterChunkCalled = true;
		}
		
		@BeforeRead
		public void beforeReadMethod(){
			beforeReadCalled = true;
		}
		
		@AfterRead
		public void afterReadMethod(Object item){
			Assert.notNull(item);
			afterReadCalled = true;
		}
		
		@OnReadError
		public void onErrorInRead(){
			onReadErrorCalled = true;
		}
		
		@BeforeProcess
		public void beforeProcess(){
			beforeProcessCalled = true;
		}
		
		@AfterProcess
		public void afterProcess(){
			afterProcessCalled = true;
		}
		
		@OnProcessError
		public void processError(){
			onProcessErrorCalled = true;
		}
		
		@BeforeWrite
		public void beforeWrite(){
			beforeWriteCalled = true;
		}
		
		@AfterWrite
		public void afterWrite(){
			afterWriteCalled = true;
		}

		@OnWriteError
		public void writeError(){
			onWriteErrorCalled = true;
		}
		
		public void onSkipInProcess(Object item, Throwable t) {
			onSkipInProcessCalled = true;
		}

		public void onSkipInRead(Throwable t) {
			onSkipInReadCalled = true;
		}

		public void onSkipInWrite(Object item, Throwable t) {
			onSkipInWriteCalled = true;
		}
		
	}
}
