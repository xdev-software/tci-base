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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Used to dynamically search for classes.
 * <p>
 * Utilizes caching and only searches for these classes once.
 * </p>
 * It's recommended to instantiate this in a static field/constant for optimal performance.
 */
public abstract class AbstractDynamicClassFinder<SELF extends AbstractDynamicClassFinder<SELF>>
	implements Supplier<Set<String>>
{
	protected Supplier<AnnotatedClassFinder> classFinderProvider = AnnotatedClassFinder::new;
	protected final Set<AnnotatedClassFinderSearch> annotatedClassFinderSearches = new HashSet<>();
	protected final Set<String> additionalClasses = new HashSet<>();
	
	protected Set<String> cache;
	
	public SELF withClassFinderProvider(
		final Supplier<AnnotatedClassFinder> classFinderProvider)
	{
		this.classFinderProvider = Objects.requireNonNull(classFinderProvider);
		return this.self();
	}
	
	public SELF withSearchForAnnotatedClasses(
		final String basePackage,
		final Class<? extends Annotation> annotationClazz)
	{
		return this.withSearchForAnnotatedClasses(basePackage, Set.of(annotationClazz));
	}
	
	public SELF withSearchForAnnotatedClasses(
		final Set<String> basePackages,
		final Class<? extends Annotation> annotationClazz)
	{
		return this.withSearchForAnnotatedClasses(basePackages, Set.of(annotationClazz));
	}
	
	public SELF withSearchForAnnotatedClasses(
		final String basePackage,
		final Set<Class<? extends Annotation>> annotationClasses)
	{
		return this.withSearchForAnnotatedClasses(Set.of(basePackage), annotationClasses);
	}
	
	public SELF withSearchForAnnotatedClasses(
		final Set<String> basePackages,
		final Set<Class<? extends Annotation>> annotationClasses)
	{
		this.annotatedClassFinderSearches.add(new AnnotatedClassFinderSearch(basePackages, annotationClasses));
		return this.self();
	}
	
	public SELF withAdditionalClasses(final Collection<String> classNames)
	{
		this.additionalClasses.addAll(classNames);
		return this.self();
	}
	
	public SELF withAdditionalClass(final String className)
	{
		this.additionalClasses.add(className);
		return this.self();
	}
	
	@Override
	public Set<String> get()
	{
		if(this.cache == null)
		{
			final AnnotatedClassFinder annotatedClassFinder = this.classFinderProvider.get();
			this.cache = Stream.concat(
					this.annotatedClassFinderSearches.stream()
						.map(search -> annotatedClassFinder.find(search.basePackages(), search.annotationClasses()))
						.flatMap(List::stream)
						.distinct()
						.map(Class::getName),
					this.additionalClasses.stream())
				.collect(Collectors.toSet());
		}
		return this.cache;
	}
	
	@SuppressWarnings("unchecked")
	protected SELF self()
	{
		return (SELF)this;
	}
	
	protected record AnnotatedClassFinderSearch(
		Set<String> basePackages,
		Set<Class<? extends Annotation>> annotationClasses
	)
	{
	}
}
