package net.minecraft.scalar.rosettaenchanttable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface ModProperty {
	String comment() default "";
	int defaultInt() default 0;
	String defaultString() default "";
	double defaultDouble() default 0d;
	boolean defaultBoolean() default false;
}
