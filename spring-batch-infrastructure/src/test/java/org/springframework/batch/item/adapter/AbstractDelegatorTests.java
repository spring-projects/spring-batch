package org.springframework.batch.item.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator.InvocationTargetThrowableWrapper;
import org.springframework.util.Assert;

/**
 * Tests for {@link AbstractMethodInvokingDelegator}
 * 
 * @author Robert Kasanicky
 */
public class AbstractDelegatorTests {

	private static class ConcreteDelegator extends AbstractMethodInvokingDelegator<Foo> {
	}

	private AbstractMethodInvokingDelegator<Foo> delegator = new ConcreteDelegator();

	private Foo foo = new Foo("foo", 1);

	@Before
	public void setUp() throws Exception {
		delegator.setTargetObject(foo);
		delegator.setArguments(null);
	}

	/**
	 * Regular use - calling methods directly and via delegator leads to same
	 * results
	 */
	@Test
	public void testDelegation() throws Exception {
		delegator.setTargetMethod("getName");
		delegator.afterPropertiesSet();

		assertEquals(foo.getName(), delegator.invokeDelegateMethod());
	}

	/**
	 * Regular use - calling methods directly and via delegator leads to same
	 * results
	 */
	@Test
	public void testDelegationWithArgument() throws Exception {
		delegator.setTargetMethod("setName");
		final String NEW_FOO_NAME = "newFooName";
		delegator.afterPropertiesSet();

		delegator.invokeDelegateMethodWithArgument(NEW_FOO_NAME);
		assertEquals(NEW_FOO_NAME, foo.getName());

		// using the arguments setter should work equally well
		foo.setName("foo");
		Assert.state(!foo.getName().equals(NEW_FOO_NAME));
		delegator.setArguments(new Object[] { NEW_FOO_NAME });
		delegator.afterPropertiesSet();
		delegator.invokeDelegateMethod();
		assertEquals(NEW_FOO_NAME, foo.getName());
	}

	/**
	 * Null argument value doesn't cause trouble when validating method
	 * signature.
	 */
	@Test
	public void testDelegationWithCheckedNullArgument() throws Exception {
		delegator.setTargetMethod("setName");
		delegator.setArguments(new Object[] { null });
		delegator.afterPropertiesSet();
		delegator.invokeDelegateMethod();
		assertNull(foo.getName());
	}

	/**
	 * Regular use - calling methods directly and via delegator leads to same
	 * results
	 */
	@Test
	public void testDelegationWithMultipleArguments() throws Exception {
		FooService fooService = new FooService();
		delegator.setTargetObject(fooService);
		delegator.setTargetMethod("processNameValuePair");
		delegator.afterPropertiesSet();

		final String FOO_NAME = "fooName";
		final int FOO_VALUE = 12345;

		delegator.invokeDelegateMethodWithArguments(new Object[] { FOO_NAME, FOO_VALUE });
		Foo foo = (Foo) fooService.getProcessedFooNameValuePairs().get(0);
		assertEquals(FOO_NAME, foo.getName());
		assertEquals(FOO_VALUE, foo.getValue());
	}

	/**
	 * Exception scenario - target method is not declared by target object.
	 */
	@Test
	public void testInvalidMethodName() throws Exception {
		delegator.setTargetMethod("not-existing-method-name");

		try {
			delegator.afterPropertiesSet();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}

		try {
			delegator.invokeDelegateMethod();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - target method is called with invalid arguments.
	 */
	@Test
	public void testInvalidArgumentsForExistingMethod() throws Exception {
		delegator.setTargetMethod("setName");
		delegator.afterPropertiesSet();
		try {
			delegator.invokeDelegateMethodWithArgument(new Object());
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - target method is called with incorrect number of
	 * arguments.
	 */
	@Test
	public void testTooFewArguments() throws Exception {
		delegator.setTargetMethod("setName");
		delegator.afterPropertiesSet();
		try {
			// single argument expected but none provided
			delegator.invokeDelegateMethod();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testTooManyArguments() throws Exception {
		delegator.setTargetMethod("setName");
		// single argument expected but two provided
		delegator.invokeDelegateMethodWithArguments(new Object[] { "name", "anotherName" });
		assertEquals("name", foo.getName());
	}

	/**
	 * Exception scenario - incorrect static arguments set.
	 */
	@Test
	public void testIncorrectNumberOfStaticArguments() throws Exception {
		delegator.setTargetMethod("setName");

		// incorrect argument count
		delegator.setArguments(new Object[] { "first", "second" });
		try {
			delegator.afterPropertiesSet();
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}

		// correct argument count, but invalid argument type
		delegator.setArguments(new Object[] { new Object() });
		try {
			delegator.afterPropertiesSet();
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - target method is successfully invoked but throws
	 * exception. Such 'business' exception should be re-thrown as is (without
	 * wrapping).
	 */
	@Test
	public void testDelegateException() throws Exception {
		delegator.setTargetMethod("fail");
		delegator.afterPropertiesSet();
		try {
			delegator.invokeDelegateMethod();
			fail();
		}
		catch (Exception expected) {
			assertEquals(Foo.FAILURE_MESSAGE, expected.getMessage());
		}

	}

	/**
	 * Exception scenario - target method is successfully invoked but throws a
	 * {@link Throwable} (not an {@link Exception}).
	 */
	@Test
	public void testDelegateThrowable() throws Exception {
		delegator.setTargetMethod("failUgly");
		delegator.afterPropertiesSet();
		try {
			delegator.invokeDelegateMethod();
			fail();
		}
		catch (InvocationTargetThrowableWrapper expected) {
			assertEquals(Foo.UGLY_FAILURE_MESSAGE, expected.getCause().getMessage());
		}
	}

	@SuppressWarnings("unused")
	private static class Foo {

		public static final String FAILURE_MESSAGE = "Foo Failure!";

		public static final String UGLY_FAILURE_MESSAGE = "Ugly Foo Failure!";

		private String name;

		private int value;

		public Foo(String name, int value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getValue() {
			return value;
		}

		public void fail() throws Exception {
			throw new Exception(FAILURE_MESSAGE);
		}

		public void failUgly() throws Throwable {
			throw new Throwable(UGLY_FAILURE_MESSAGE);
		}

	}

	private static class FooService {

		private List<Foo> processedFooNameValuePairs = new ArrayList<Foo>();

		@SuppressWarnings("unused")
		public void processNameValuePair(String name, int value) {
			processedFooNameValuePairs.add(new Foo(name, value));
		}

		@SuppressWarnings("unused")
		public void processNameValuePair(String name, String value) {
			processedFooNameValuePairs.add(new Foo(name, new Integer(value)));
		}

		public List<Foo> getProcessedFooNameValuePairs() {
			return processedFooNameValuePairs;
		}

	}

}
