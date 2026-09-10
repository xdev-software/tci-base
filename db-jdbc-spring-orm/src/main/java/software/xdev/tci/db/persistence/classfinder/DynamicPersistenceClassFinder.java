/*
 * Copyright © 2025 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.tci.db.persistence.classfinder;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;


/**
 * Used to dynamically search for (persistence) classes.
 * <p>
 * Utilizes caching and only searches for these classes once.
 * </p>
 * It's recommended to instantiate this in a static field/constant for optimal performance.
 */
public class DynamicPersistenceClassFinder extends AbstractDynamicClassFinder<DynamicPersistenceClassFinder>
{
	// Taken from Hibernate's ClassFileArchiveEntryHandler
	public static final Set<Class<? extends Annotation>> DEFAULT_PERSISTENCE_ANNOTATIONS = Set.of(
		// MODELS
		Entity.class,
		MappedSuperclass.class,
		Embeddable.class,
		// CONVERTER
		Converter.class);
	
	public DynamicPersistenceClassFinder withSearchForPersistenceClasses(final String basePackage)
	{
		return this.withSearchForAnnotatedClasses(basePackage, DEFAULT_PERSISTENCE_ANNOTATIONS);
	}
	
	public DynamicPersistenceClassFinder withSearchForPersistenceClasses(final String... packages)
	{
		return this.withSearchForPersistenceClasses(Arrays.stream(packages).collect(Collectors.toSet()));
	}
	
	public DynamicPersistenceClassFinder withSearchForPersistenceClasses(final Set<String> basePackages)
	{
		return this.withSearchForAnnotatedClasses(basePackages, DEFAULT_PERSISTENCE_ANNOTATIONS);
	}
}
