package org.springframework.batch.core.scope;

import java.util.ArrayList;
import java.util.List;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TestAdvice {

	public static final List<String> names = new ArrayList<String>();
	
	@AfterReturning(pointcut="execution(String org.springframework.batch.core.scope.Collaborator+.getName(..))", returning="name")
	public void registerCollaborator(String name) {
		names.add(name);
	}
	
	
}
