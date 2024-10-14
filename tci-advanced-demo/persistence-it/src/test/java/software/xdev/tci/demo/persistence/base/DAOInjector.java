package software.xdev.tci.demo.persistence.base;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import software.xdev.tci.demo.persistence.jpa.dao.BaseDAO;
import software.xdev.tci.demo.persistence.jpa.dao.TransactionReflector;
import software.xdev.tci.demo.persistence.util.TransactionExecutor;


/**
 * Injects DAO into a test class.
 * <p>
 * Also handles @{@link Transactional} by using proxies.
 * </p>
 */
public class DAOInjector
{
	private static final Logger LOG = LoggerFactory.getLogger(DAOInjector.class);
	
	public void doInjections(
		final Class<?> clazz,
		final Supplier<EntityManager> emSupplier,
		final Supplier<Object> instanceSupplier)
	{
		this.collectAllDeclaredFields(clazz).stream()
			.filter(field -> field.isAnnotationPresent(Autowired.class))
			.filter(field -> BaseDAO.class.isAssignableFrom(field.getType()))
			.forEach(field -> {
				final Class<?> fieldType = field.getType();
				final EntityManager em = emSupplier.get();
				
				final Object originalDAO = this.createDAO(fieldType, em);
				final Object dao = this.collectAllDeclaredMethods(fieldType)
					.stream()
					.anyMatch(m -> m.isAnnotationPresent(Transactional.class))
					? this.createProxiedDAO(fieldType, em, originalDAO)
					: originalDAO;
				
				this.collectAllDeclaredFields(fieldType)
					.stream()
					.filter(f -> TransactionReflector.class.equals(f.getType()))
					.forEach(f -> this.setIntoField(dao, f, new TransactionReflector()
					{
						@Override
						public void runWithTransaction(final Runnable runnable)
						{
							new TransactionExecutor(em).execWithTransaction(runnable);
						}
						
						@Override
						public <T> T runWithTransaction(final Supplier<T> supplier)
						{
							return new TransactionExecutor(em).execWithTransaction(supplier);
						}
					}));
				
				this.setIntoField(instanceSupplier.get(), field, dao);
			});
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	private Object createProxiedDAO(final Class<?> fieldType, final EntityManager em, final Object original)
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
					throw new RuntimeException(e);
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
					BaseDAO.class.getDeclaredField("em"),
					em);
			}
			catch(final NoSuchMethodException | InstantiationException | IllegalAccessException
						| InvocationTargetException | NoSuchFieldException e2)
			{
				throw new RuntimeException("Failed to proxy dao", e2);
			}
		}
	}
	
	@SuppressWarnings("PMD.PreserveStackTrace")
	private Object createDAO(final Class<?> fieldType, final EntityManager em)
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
					BaseDAO.class.getDeclaredField("em"),
					em);
			}
			catch(final NoSuchFieldException | NoSuchMethodException | InstantiationException
						| IllegalAccessException | InvocationTargetException e2)
			{
				throw new IllegalArgumentException("Unable to find inject em for " + fieldType, e2);
			}
		}
	}
	
	private Set<Field> collectAllDeclaredFields(final Class<?> clazz)
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
	
	private Set<Method> collectAllDeclaredMethods(final Class<?> clazz)
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
	
	private Object setIntoField(final Object instance, final Field field, final Object newValue)
	{
		field.setAccessible(true);
		try
		{
			field.set(instance, newValue);
		}
		catch(final IllegalAccessException iae)
		{
			throw new RuntimeException(iae);
		}
		return instance;
	}
}
