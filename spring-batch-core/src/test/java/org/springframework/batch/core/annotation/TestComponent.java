/**
 * 
 */
package org.springframework.batch.core.annotation;

/**
 * @author Lucas Ward
 *
 */
@BatchComponent
public class TestComponent {

	private static boolean beforeStepCalled = false;
	
	private static boolean afterStepCalled = false;
	
	public static boolean isBeforeStepCalled(){
		return beforeStepCalled;
	}
	
	public static boolean isAfterStepCalled(){
		return afterStepCalled;
	}
	
	@BeforeStep
	public void beforeStep(){
		beforeStepCalled = true;
	}
	
	@AfterStep
	public void afterStep(){
		afterStepCalled = true;
	}
}
