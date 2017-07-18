package net.kodehawa.mantarobot.core.listeners.operations;

import br.com.brjdevs.java.utils.async.threads.builder.ThreadBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;
import net.kodehawa.mantarobot.core.listeners.external.OptimizedListener;
import net.kodehawa.mantarobot.utils.TimeAmount;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

public class InteractiveOperation {
	private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(
		new ThreadBuilder().setName("InteractiveOperations Executor")
	);

	private static final Map<String, InteractiveOperation> OPERATIONS = new ConcurrentHashMap<>();
	private static final EventListener LISTENER = new OptimizedListener<GuildMessageReceivedEvent>(
		GuildMessageReceivedEvent.class) {
		@Override
		public void event(GuildMessageReceivedEvent event) {
			String id = event.getChannel().getId();
			InteractiveOperation operation = OPERATIONS.get(id);

			if (operation != null) {
				if (operation.timeoutFuture != null) {
					operation.timeoutFuture.cancel(true);
					operation.timeoutFuture = null;
				}

				if (operation.onMessage.test(event)) {
					OPERATIONS.remove(id, operation);
				} else {
					scheduleTimeout(operation, false);
				}
			}
		}
	};

	public static InteractiveOperationBuilder builder() {
		return new InteractiveOperationBuilder();
	}

	public static EventListener listener() {
		return LISTENER;
	}

	public static void stopOperation(String channelId) {
		InteractiveOperation operation = OPERATIONS.remove(channelId);

		if (operation != null) {
			if (operation.timeoutFuture != null) {
				operation.timeoutFuture.cancel(true);
				operation.timeoutFuture = null;
			}

			if (operation.onRemoved != null) {
				operation.onRemoved.run();
			}
		}
	}

	private static void scheduleTimeout(InteractiveOperation operation, boolean first) {
		TimeAmount timeAmount = first ? operation.initialTimeout : operation.increasingTimeout;

		if (timeAmount == null) return;

		operation.timeoutFuture = EXECUTOR.schedule(
			() -> {
				OPERATIONS.remove(operation.channelId, operation);
				if (operation.onTimeout != null) {
					operation.onTimeout.run();
				}
			}, timeAmount.getAmount(), timeAmount.getUnit()
		);
	}

	private final String channelId;
	private final TimeAmount increasingTimeout;
	private final TimeAmount initialTimeout;
	private final Predicate<GuildMessageReceivedEvent> onMessage;
	private final Runnable onTimeout, onRemoved;
	private Future<?> timeoutFuture;

	InteractiveOperation(String channelId, TimeAmount initialTimeout, TimeAmount increasingTimeout, Predicate<GuildMessageReceivedEvent> onMessage, Runnable onTimeout, Runnable onRemoved) {
		this.channelId = channelId;
		this.initialTimeout = initialTimeout;
		this.increasingTimeout = increasingTimeout;
		this.onMessage = onMessage;
		this.onRemoved = onRemoved;
		this.onTimeout = onTimeout;

		if (OPERATIONS.containsKey(channelId))
			throw new IllegalStateException("Operation already happening at channelId");

		OPERATIONS.put(channelId, this);

		scheduleTimeout(this, true);
	}
}
