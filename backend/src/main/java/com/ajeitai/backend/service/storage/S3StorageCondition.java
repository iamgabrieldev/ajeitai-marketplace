package com.ajeitai.backend.service.storage;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(S3StorageConditionImpl.class)
public @interface S3StorageCondition {
}
