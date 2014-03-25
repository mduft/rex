/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to denote a method that can append command related help to a
 * {@link StringBuilder}. Signature of the target method has to be
 * {@code static void m(StringBuilder)}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface HelpAppender {

}
