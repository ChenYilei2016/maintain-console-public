package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireConsoleRole {
    ConsoleRole value();
}
