/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.item.transform;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;

import junit.framework.TestCase;

/**
 * These functional tests were created for the Reference Documentation and show how various
 * combinations of ItemTransformer can be used with an ItemTransformerItemWriter.
 * 
 * @author Lucas Ward
 *
 */
public class ItemTransformerItemWriterFunctionalTests extends TestCase {
	
	public void testTransform() throws Exception{
		
		ItemTransformerItemWriter itemTransformerItemWriter = new ItemTransformerItemWriter();
		itemTransformerItemWriter.setItemTransformer(new FooTransformer());
		itemTransformerItemWriter.setDelegate(new BarWriter());
		itemTransformerItemWriter.write(new Foo());
	}
	
	public void testComposite() throws Exception{
		
		CompositeItemTransformer compositeTransformer = new CompositeItemTransformer();
		List<ItemTransformer> itemTransformers = new ArrayList<ItemTransformer>();
		itemTransformers.add(new FooTransformer());
		itemTransformers.add(new BarTransformer());
		compositeTransformer.setItemTransformers(itemTransformers);
		ItemTransformerItemWriter itemTransformerItemWriter = new ItemTransformerItemWriter();
		itemTransformerItemWriter.setItemTransformer(compositeTransformer);
		itemTransformerItemWriter.setDelegate(new FoobarWriter());
		itemTransformerItemWriter.write(new Foo());
	}
	
	private static class Foo {
		
	}
	
	private static class Bar {
		public Bar(Foo foo) {
		}
	}
	
	private static class Foobar{
		public Foobar(Bar bar){}
	}
	
	public class FooTransformer implements ItemTransformer{

		//Preform simple transformation, convert a Foo to a Barr
		public Object transform(Object item) throws Exception {
			assertTrue(item instanceof Foo);
			Foo foo = (Foo)item;
			return new Bar(foo);
		}
	}
	
	public class BarTransformer implements ItemTransformer{

		public Object transform(Object item) throws Exception {
			assertTrue(item instanceof Bar);
			return new Foobar((Bar)item);
		}
	}
	
	private static class BarWriter implements ItemWriter<Bar>{

		public void write(Bar item) throws Exception {
		  assertTrue(item instanceof Bar);
		}
		
		public void clear() throws ClearFailedException {
		}

		public void flush() throws FlushFailedException {
		}
		
	}
	
	private static class FoobarWriter implements ItemWriter<Foobar>{

		public void write(Foobar item) throws Exception {
		  assertTrue(item instanceof Foobar);
		}
		
		public void clear() throws ClearFailedException {
		}

		public void flush() throws FlushFailedException {
		}
	}
}
