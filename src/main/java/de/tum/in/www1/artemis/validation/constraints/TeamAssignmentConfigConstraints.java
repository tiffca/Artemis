package de.tum.in.www1.artemis.validation.constraints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import de.tum.in.www1.artemis.validation.TeamAssignmentConfigValidator;

@Constraint(validatedBy = TeamAssignmentConfigValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// TODO: Martin Wauligmann - Describe what the purpose of this is
public @interface TeamAssignmentConfigConstraints {

    String message() default "{de.tum.in.www1.artemis.validation.constraints.TeamAssignmentConfigConstraints}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}