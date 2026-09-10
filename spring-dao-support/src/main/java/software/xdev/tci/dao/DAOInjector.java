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
package software.xdev.tci.dao;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import software.xdev.tci.db.persistence.TransactionExecutor;


/**
 * Injects DAO into a test class.
 * <p>
 * Also handles @{@link Transactional} by using proxies.
 * </p>
 */
public class DAOInjector
{
	private static final Logger LOG = LoggerFactory.getLogger(DAOInjector.class);
	
	protected final Class<?> baseDAOClazz;
	protected final ThrowingFieldSupplier fEntityManagerInBaseDAOSupplier;
	protected final HandleSpecialFields handleSpecialFields;
	
	public DAOInjector(
		final Class<?> baseDAOClazz,
		final ThrowingFieldSupplier fEntityManagerInBaseDAOSupplier,
		final HandleSpecialFields handleSpecialFields)
	{
		this.baseDAOClazz = Objects.requireNonNull(baseDAOClazz);
		this.fEntityManagerInBaseDAOSupplier = Objects.requireNonNull(fEntityManagerInBaseDAOSupplier);
		this.handleSpecialFields = handleSpecialFields;
	}
	
	public void doInjections(
		final Class<?> clazz,
		final Supplier<EntityManager> emSupplier,
		final Supplier<Object> instanceSupplier)
	{
		this.collectAllDeclaredFields(clazz).stream()
			.filter(field -> field.isAnnotationPresent(Autowired.class))
			.filter(field -> this.baseDAOClazz.isAssignableFrom(field.getType()))
			.forEach(field -> {
				final Class<?> fieldType = field.getType();
				final EntityManager em = emSupplier.get();
				
				final Object originalDAO = this.createDAO(fieldType, em);
				final Object dao = this.collectAllDeclaredMethods(fieldType)
					.stream()
					.anyMatch(m -> m.isAnnotationPresent(Transactional.class))
					? this.createProxiedDAO(fieldType, em, originalDAO)
					: originalDAO;
				
				if(this.handleSpecialFields != null)
				{
					this.handleSpecialFields.handle(this, this.collectAllDeclaredFields(fieldType), dao, em);
				}
				
				this.setIntoField(instanceSupplier.get(), field, dao);
			});
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	protected Object createProxiedDAO(final Class<?> fieldType, final EntityManager em, final Object original)
	{
		// java.lang.reflect.Proxy only proxies interfaces and doesn't work here!
		// https://stackoverflow.com/a/3292208
		final ProxyFactory factory = new ProxyFactory();
		factory.setSuperclass(fieldType);
		factory.setFilter(m -> !Modifier.isStatic(m.getModifiers())
			&& m.isAnnotationPresent(Transactional.class));
		final MethodHandler methodHandler = (self, thisMethod, proceed, args) ->
		{
			final Supplier<Object> invoker = () -> {
				try
				{
					return thisMethod.invoke(original, args);
				}
				catch(final IllegalAccessException | InvocationTargetException e)
				{
					throw new IllegalStateException("Failed to invoke", e);
				}
			};
			if(em.getTransaction().isActive())
			{
				return invoker.get();
			}
			return new TransactionExecutor(em).execWithTransaction(invoker);
		};
		
		try
		{
			return factory.create(new Class<?>[]{EntityManager.class}, new Object[]{em}, methodHandler);
		}
		catch(final NoSuchMethodException | InstantiationException | IllegalAccessException
					| InvocationTargetException e)
		{
			LOG.debug("Unable to find EntityManager constructor for {}, using fallback", fieldType, e);
			try
			{
				return this.setIntoField(
					factory.create(new Class<?>[0], new Object[0], methodHandler),
					this.fEntityManagerInBaseDAOSupplier.field(),
					em);
			}
			catch(final NoSuchFieldException | NoSuchMethodException | InstantiationException | IllegalAccessException
						| InvocationTargetException e2)
			{
				throw new IllegalStateException("Failed to proxy dao", e2);
			}
		}
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	protected Object createDAO(final Class<?> fieldType, final EntityManager em)
	{
		try
		{
			return fieldType
				.getConstructor(EntityManager.class)
				.newInstance(em);
		}
		catch(final NoSuchMethodException | InstantiationException
					| IllegalAccessException | InvocationTargetException e)
		{
			LOG.debug("Unable to find EntityManager constructor for {}, using fallback", fieldType, e);
			try
			{
				return this.setIntoField(
					fieldType.getConstructor().newInstance(),
					this.fEntityManagerInBaseDAOSupplier.field(),
					em);
			}
			catch(final NoSuchFieldException | NoSuchMethodException | InstantiationException
						| IllegalAccessException | InvocationTargetException e2)
			{
				throw new IllegalArgumentException("Unable to find inject em for " + fieldType, e2);
			}
		}
	}
	
	protected Set<Field> collectAllDeclaredFields(final Class<?> clazz)
	{
		final Set<Field> fields = new HashSet<>();
		Class<?> currentClazz = clazz;
		while(currentClazz != null && !Object.class.equals(currentClazz))
		{
			fields.addAll(Arrays.asList(currentClazz.getDeclaredFields()));
			currentClazz = currentClazz.getSuperclass();
		}
		return fields;
	}
	
	protected Set<Method> collectAllDeclaredMethods(final Class<?> clazz)
	{
		final Set<Method> methods = new HashSet<>();
		Class<?> currentClazz = clazz;
		while(currentClazz != null && !Object.class.equals(currentClazz))
		{
			methods.addAll(Arrays.asList(currentClazz.getDeclaredMethods()));
			currentClazz = currentClazz.getSuperclass();
		}
		return methods;
	}
	
	@SuppressWarnings("java:S3011")
	public Object setIntoField(final Object instance, final Field field, final Object newValue)
	{
		field.setAccessible(true);
		try
		{
			field.set(instance, newValue);
		}
		catch(final IllegalAccessException iae)
		{
			throw new IllegalStateException("Failed to set into field", iae);
		}
		return instance;
	}
	
	@FunctionalInterface
	public interface ThrowingFieldSupplier
	{
		Field field() throws NoSuchFieldException;
	}
	
	
	@FunctionalInterface
	public interface HandleSpecialFields
	{
		void handle(DAOInjector self, Set<Field> fields, Object dao, EntityManager em);
	}
}
