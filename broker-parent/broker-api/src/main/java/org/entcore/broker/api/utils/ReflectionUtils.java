package org.entcore.broker.api.utils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Utility class for reflection operations used by broker components
 */
public class ReflectionUtils {
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    private ReflectionUtils(){}
    /**
     * Find an annotation on a method, checking interfaces and parent classes if not found directly
     * 
     * @param <A> The type of annotation to find
     * @param annotationClass The class of the annotation to search for
     * @param targetClass The class that declares or inherits the method
     * @param method The method to find the annotation on
     * @return Optional containing the annotation if found, empty otherwise
     */
    public static <A extends Annotation> Optional<A> getMethodAnnotation(
            Class<A> annotationClass, 
            Class<?> targetClass, 
            Method method) {
        
        // First check if the annotation is directly on the method
        A annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        
        // Check interfaces implemented by the class
        annotation = findAnnotationInInterfaces(annotationClass, targetClass.getInterfaces(), method);
        if (annotation != null) {
            return Optional.of(annotation);
        }
        
        // Check parent class if available
        if (targetClass.getSuperclass() != null && !targetClass.getSuperclass().equals(Object.class)) {
            return getMethodAnnotation(annotationClass, targetClass.getSuperclass(), method);
        }
        
        return Optional.empty();
    }
    
    /**
     * Searches for the annotation in interfaces implemented by a class
     * 
     * @param <A> The type of annotation to find
     * @param annotationClass The class of the annotation to search for
     * @param interfaces Array of interfaces to check
     * @param method The method to find the annotation on
     * @return The annotation if found, null otherwise
     */
    private static <A extends Annotation> A findAnnotationInInterfaces(
            Class<A> annotationClass, 
            Class<?>[] interfaces, 
            Method method) {
            
        for (Class<?> iface : interfaces) {
            try {
                Method interfaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                A annotation = interfaceMethod.getAnnotation(annotationClass);
                if (annotation != null) {
                    return annotation;
                }
                
                // Recursively check interfaces implemented by this interface
                annotation = findAnnotationInInterfaces(annotationClass, iface.getInterfaces(), method);
                if (annotation != null) {
                    return annotation;
                }
            } catch (NoSuchMethodException e) {
                // Method not found in this interface, continue with next interface
            }
        }
        return null;
    }
}