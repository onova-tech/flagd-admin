package tech.onova.flagd_admin_server.infrastructure.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.onova.flagd_admin_server.infrastructure.annotation.Log;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    private LoggingAspect loggingAspect;
    private ProceedingJoinPoint joinPoint;
    private Log logAnnotation;

    @BeforeEach
    void setUp() {
        loggingAspect = new LoggingAspect();
        joinPoint = mock(ProceedingJoinPoint.class);
        logAnnotation = mock(Log.class);
    }

    @Test
    void shouldLogMethodExecutionWithReturnValue() throws Throwable {
        // Given
        String methodName = "testMethod";
        String className = "TestClass";
        Object[] args = {"arg1", 42};
        String expectedResult = "result";
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = loggingAspect.logAround(joinPoint, logAnnotation);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
        verify(joinPoint).getTarget();
    }

    @Test
    void shouldLogMethodExecutionWithVoidReturn() throws Throwable {
        // Given
        String methodName = "voidMethod";
        String className = "TestClass";
        Object[] args = {};
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(null);

        // When
        Object result = loggingAspect.logAround(joinPoint, logAnnotation);

        // Then
        assertThat(result).isNull();
        verify(joinPoint).proceed();
    }

    @Test
    void shouldLogMethodExecutionWithExceptions() throws Throwable {
        // Given
        String methodName = "exceptionMethod";
        String className = "TestClass";
        Object[] args = {"arg1"};
        RuntimeException expectedException = new RuntimeException("Test exception");
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> loggingAspect.logAround(joinPoint, logAnnotation))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test exception")
            .isSameAs(expectedException);

        verify(joinPoint).proceed();
    }

    @Test
    void shouldLogMethodExecutionWithNoArguments() throws Throwable {
        // Given
        String methodName = "noArgsMethod";
        String className = "TestClass";
        Object[] args = new Object[0];
        String expectedResult = "result";
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // When
        Object result = loggingAspect.logAround(joinPoint, logAnnotation);

        // Then
        assertThat(result).isEqualTo(expectedResult);
        verify(joinPoint).proceed();
    }

    @Test
    void shouldHandleNullArgs() throws Throwable {
        // Given
        String methodName = "nullArgsMethod";
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("result");

        // When
        Object result = loggingAspect.logAround(joinPoint, logAnnotation);

        // Then
        assertThat(result).isEqualTo("result");
        verify(joinPoint).proceed();
    }

    @Test
    void shouldNotSwallowOriginalException() throws Throwable {
        // Given
        String methodName = "exceptionMethod";
        CustomException originalException = new CustomException("Original exception");
        TestClass target = new TestClass();
        
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(mock(org.aspectj.lang.Signature.class));
        when(joinPoint.getSignature().getName()).thenReturn(methodName);
        when(joinPoint.getArgs()).thenReturn(new Object[0]);
        when(joinPoint.proceed()).thenThrow(originalException);

        // When & Then
        assertThatThrownBy(() -> loggingAspect.logAround(joinPoint, logAnnotation))
            .isInstanceOf(CustomException.class)
            .hasMessage("Original exception")
            .isSameAs(originalException);

        verify(joinPoint).proceed();
    }

    // Test class for signature testing
    private static class TestClass {
        // Empty class for testing
    }

    // Custom exception for testing
    private static class CustomException extends RuntimeException {
        public CustomException(String message) {
            super(message);
        }
    }
}