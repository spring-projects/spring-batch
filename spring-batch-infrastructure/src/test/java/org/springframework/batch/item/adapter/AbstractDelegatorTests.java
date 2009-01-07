package org.springframework.batch.item.adapter;

import junit.framework.TestCase;

import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator;
import org.springframework.batch.item.adapter.DynamicMethodInvocationException;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.sample.FooService;
import org.springframework.util.Assert;

/**
 * Tests for {@link AbstractMethodInvokingDelegator}
 * 
 * @author Robert Kasanicky
 */
public class AbstractDelegatorTests extends TestCase {

	private static class ConcreteDelegator extends AbstractMethodInvokingDelegator<Foo> {
	}

	private AbstractMethodInvokingDelegator<Foo> delegator = new ConcreteDelegator();

	private Foo foo = new Foo(0, "foo", 1);

	protected void setUp() throws Exception {
		delegator.setTargetObject(foo);
		delegator.setArguments(null);
	}

	/**
	 * Regular use - calling methods directly and via delegator leads to same
	 * results
	 */
	public void testDelegation() throws Exception {
		delegator.setTargetMethod("getName");
		delegator.afterPropertiesSet();

		assertEquals(foo.getName(), delegator.invokeDelegateMethod());
	}

	/**
	 * Regular use - calling methods directly and via delegator leads to same
	 * results
	 */
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
	public void testDelegationWithMultipleArguments() throws Exception {
		FooService fooService = new FooService();
		delegator.setTargetObject(fooService);
		delegator.setTargetMethod("processNameValuePair");
		delegator.afterPropertiesSet();

		final String FOO_NAME = "fooName";
		final int FOO_VALUE = 12345;

		delegator.invokeDelegateMethodWithArguments(new Object[] { FOO_NAME, Integer.valueOf(FOO_VALUE) });
		Foo foo = (Foo) fooService.getProcessedFooNameValuePairs().get(0);
		assertEquals(FOO_NAME, foo.getName());
		assertEquals(FOO_VALUE, foo.getValue());
	}

	/**
	 * Exception scenario - target method is not declared by target object.
	 */
	public void testInvalidMethodName() throws Exception {
		delegator.setTargetMethod("not-existing-method-name");

		try {
			delegator.afterPropertiesSet();
			fail();
		}
		catch (IllegalStateException e) {
			// expected
		}

		try {
			delegator.invokeDelegateMethod();
			fail();
		}
		catch (DynamicMethodInvocationException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - target method is called with invalid arguments.
	 */
	public void testInvalidArgumentsForExistingMethod() throws Exception {
		delegator.setTargetMethod("setName");
		delegator.afterPropertiesSet();
		try {
			delegator.invokeDelegateMethodWithArgument(new Object());
			fail();
		}
		catch (DynamicMethodInvocationException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - target method is called with incorrect number of
	 * arguments.
	 */
	public void testIncorrectArgumentCount() throws Exception {
		delegator.setTargetMethod("setName");
		delegator.afterPropertiesSet();
		try {
			// single argument expected but none provided
			delegator.invokeDelegateMethod();
			fail();
		}
		catch (DynamicMethodInvocationException e) {
			// expected
		}

		try {
			// single argument expected but two provided
			delegator.invokeDelegateMethodWithArguments(new Object[] { "name", "anotherName" });
			fail();
		}
		catch (DynamicMethodInvocationException e) {
			// expected
		}
	}

	/**
	 * Exception scenario - incorrect static arguments set.
	 */
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

}
