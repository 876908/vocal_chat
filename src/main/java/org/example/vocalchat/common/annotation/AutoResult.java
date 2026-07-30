package org.example.vocalchat.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 类或方法，使返回值自动包装为 {@link org.example.vocalchat.common.result.BaseResult}。
 *
 * <p>放在类上：该类所有方法都会被包装</p>
 * <p>放在方法上：仅该方法会被包装（优先级高于类级别）</p>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoResult {
}