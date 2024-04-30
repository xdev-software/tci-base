package software.xdev.tci.factory.registry;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import software.xdev.tci.TCI;
import software.xdev.tci.factory.TCIFactory;


public class DefaultTCIFactoryRegistry implements TCIFactoryRegistry
{
	protected final Set<TCIFactory<?, ?>> factories = Collections.synchronizedSet(new HashSet<>());
	
	@Override
	public void register(final TCIFactory<?, ?> tciFactory)
	{
		this.factories.add(tciFactory);
	}
	
	@Override
	public void unRegister(final TCIFactory<?, ?> tciFactory)
	{
		this.factories.remove(tciFactory);
	}
	
	@Override
	public void warmUp()
	{
		final List<CompletableFuture<Void>> cfs = this.factories.stream()
			.map(f -> CompletableFuture.runAsync(f::warmUp))
			.toList();
		cfs.forEach(CompletableFuture::join);
	}
	
	@Override
	public Set<TCIFactory<?, ?>> getFactories()
	{
		return this.factories;
	}
	
	@Override
	@SuppressWarnings("java:S1452")
	public Map<TCIFactory<?, ?>, Set<TCI<?>>> getReturnedAndInUse()
	{
		return this.factories.stream()
			.filter(f -> !f.getReturnedAndInUse().isEmpty())
			.collect(Collectors.toMap(
				Function.identity(),
				f -> f.getReturnedAndInUse().stream()
					.map(x -> (TCI<?>)x)
					.collect(Collectors.toSet())));
	}
}
