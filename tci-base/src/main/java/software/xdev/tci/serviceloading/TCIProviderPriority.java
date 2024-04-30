package software.xdev.tci.serviceloading;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Priority of a provider for {@link TCIServiceLoader}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TCIProviderPriority
{
	int DEFAULT_PRIORITY = 0;
	
	int value() default DEFAULT_PRIORITY;
}
