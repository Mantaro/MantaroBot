package br.com.brjdevs.highhacks.eventbus;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Async version of {@link ASMEventBus}
 */
public class AsyncASMEventBus extends ASMEventBus {
	private final ExecutorService executor;

	public AsyncASMEventBus(ExecutorService executor, ClassLoader loader) {
		super(loader, true);
		this.executor = executor;
	}

	public AsyncASMEventBus(ExecutorService executor) {
		super(AsyncASMEventBus.class.getClassLoader(), true);
		this.executor = executor;
	}

	@Override
	public void post(Object event) {
		postOn(executor, event);
	}

	public void postOn(Executor executor, Object event) {
		executor.execute(() -> AsyncASMEventBus.super.post(event));
	}
}
