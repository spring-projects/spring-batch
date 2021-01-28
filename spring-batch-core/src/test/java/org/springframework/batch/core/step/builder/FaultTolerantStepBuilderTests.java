package org.springframework.batch.core.step.builder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class FaultTolerantStepBuilderTests {

    @Test
    public void faultTolerantReturnsSameInstance() {
        FaultTolerantStepBuilder<Object, Object> builder = new FaultTolerantStepBuilder<>(new StepBuilder("test"));
        assertEquals(builder, builder.faultTolerant());
    }
}