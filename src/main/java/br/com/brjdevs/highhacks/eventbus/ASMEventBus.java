package br.com.brjdevs.highhacks.eventbus;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FixedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.bytebuddy.jar.asm.Opcodes.*;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Event bus implementation that uses ASM to create classes to handle events to improve performance
 */
public class ASMEventBus {
	private static class Pair<Left, Right> {
		private final Left left;
		private final Right right;

		private Pair(Left left, Right right) {
			this.left = left;
			this.right = right;
		}

		public boolean equals(Object other) {
			if (other instanceof Pair) {
				Pair p = (Pair) other;
				return Objects.equals(p.left, left) && Objects.equals(p.right, right);
			}
			return false;
		}
	}

	private final ByteBuddy bb = new ByteBuddy();
	private final Map<Class<?>, List<Pair<Object, EventHandler>>> handlers;
	private final ClassLoader loader;
	private final boolean threadSafe;

	public ASMEventBus(ClassLoader loader, boolean threadSafe) {
		this.threadSafe = threadSafe;
		this.loader = loader;
		handlers = threadSafe ? new ConcurrentHashMap<>() : new HashMap<>();
	}

	public ASMEventBus(ClassLoader loader) {
		this(loader, false);
	}

	public ASMEventBus() {
		this(ASMEventBus.class.getClassLoader());
	}

	private List<Pair<Object, EventHandler>> getHandlers(Class<?> cls) {
		List<Pair<Object, EventHandler>> list = handlers.get(cls);
		if (list == null) handlers.put(cls, list = (threadSafe ? new CopyOnWriteArrayList<>() : new ArrayList<>()));
		return list;
	}

	private ASMEventHandler[] handlers(Object listener) {
		Class<?> listenerClass = listener.getClass();
		Method[] _mtds = listenerClass.getMethods();
		class Holder {
			Class<?> c;
			Method m;
		}
		List<Holder> mtds = new ArrayList<>();
		for (Method m : _mtds) {
			Listener list = m.getAnnotation(Listener.class);
			if (list == null) continue;
			Class<?>[] args = m.getParameterTypes();
			if (args.length != 1)
				throw new IllegalArgumentException(m + ": parameterTypes.length != 1");
			Class<?> arg = args[0];
			Holder h = new Holder();
			h.m = m;
			h.c = arg;
			mtds.add(h);
		}
		ASMEventHandler[] ret = new ASMEventHandler[mtds.size()];

		for (int i = 0; i < mtds.size(); i++) {
			Holder h = mtds.get(i);
			String name = listenerClass.getName() + "$1ASMEventHandler___" + h.m.getName() + "___" + h.c.getName().replace('.', '_').replace("$", "__");
			try {
				ret[i] = (ASMEventHandler) Class.forName(name, true, loader).getConstructor(Object.class).newInstance(listener);
				continue;
			} catch (ClassNotFoundException e) {
				//class not found, define it
			} catch (Exception e) {
				throw new AssertionError(e);
			}
			try {
				boolean isStatic = Modifier.isStatic(h.m.getModifiers());
				DynamicType.Builder<? extends ASMEventHandler> builder = bb.subclass(ASMEventHandler.class)
					.name(name)
					.modifiers(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC);
				if (!isStatic)
					builder = builder.defineField("instance", Object.class);
				ret[i] = builder
					.defineConstructor(ACC_PUBLIC)
					.withParameters(Object.class)
					.intercept(new ASMEventHandlerConstructor(name, isStatic))
					.method(named("handle"))
					.intercept(new ASMEventHandlerHandle(name, listenerClass.getName(), h.c.getName(), h.m, isStatic))
					.method(named("getEventClass"))
					.intercept(FixedValue.value(h.c))
					.method(named("getTargetClass"))
					.intercept(FixedValue.value(listenerClass))
					.make()
					.load(loader)
					.getLoaded()
					.getConstructor(Object.class)
					.newInstance(listener);
			} catch (Exception e) {
				throw new AssertionError("Error creating ASMEventHandler", e);
			}
		}
		return ret;
	}

	/**
	 * Posts an event
	 *
	 * @param event The event to post
	 */
	public void post(Object event) {
		Class<?> eventClass = event.getClass();
		while (eventClass != Object.class) {
			for (Pair<Object, EventHandler> h : getHandlers(eventClass)) {
				EventHandler handler = h.right;
				handler.handle(event);
			}
			eventClass = eventClass.getSuperclass();
		}
	}

	/**
	 * Registers a new listener.
	 * <p>
	 * If the listener implements {@link EventHandler}, it <b>will not</b> be treated as a normal listener,
	 * being added to the listeners list instead
	 *
	 * @param listener The listener to register
	 */
	public void register(final Object listener) {
		if (listener == null) throw new NullPointerException("listener");
		if (listener instanceof EventHandler) {
			if (listener instanceof ASMEventHandler) {
				ASMEventHandler asmeh = (ASMEventHandler) listener;
				getHandlers(asmeh.getEventClass()).add(new Pair<>(asmeh, asmeh));
				return;
			}
			getHandlers(Object.class).add(new Pair<>(listener, (EventHandler) listener));
			return;
		}
		final Class<?> listenerClass = listener.getClass();
		if (Modifier.isPublic(listenerClass.getModifiers()))
			for (ASMEventHandler h : handlers(listener))
				getHandlers(h.getEventClass()).add(new Pair<>(listener, h));
		else //Cannot access the class, reflection is the only option
			for (final Method m : listenerClass.getMethods()) {
				final Listener list = m.getAnnotation(Listener.class);
				if (list == null) continue;
				Class<?>[] params = m.getParameterTypes();
				if (params.length != 1)
					throw new IllegalArgumentException(m + ": parameterTypes.length != 1: " + m.toGenericString());
				final Class<?> evt = params[0];
				getHandlers(evt).add(new Pair<>(listener, new EventHandler() {
					@Override
					public void handle(Object event) {
						if (evt.isInstance(event)) {
							try {
								m.invoke(listener, event);
							} catch (IllegalAccessException e) {
								throw new AssertionError();
							} catch (InvocationTargetException e) {
								Throwable cause = e.getCause();
								if (cause instanceof Error)
									throw (Error) cause;
								if (cause instanceof RuntimeException)
									throw (RuntimeException) cause;
								throw new RuntimeException(cause);
							}
						}
					}

					@Override
					public String toString() {
						return "ReflectionEventHandler{" + getClass().getName() + " -> " + listenerClass.getName() + "}";
					}
				}));
			}
	}

	/**
	 * Unregisters a listener
	 *
	 * @param listener The listener to unregister
	 */
	public void unregister(Object listener) {
		if (listener == null) throw new NullPointerException("listener");
		for (List<Pair<Object, EventHandler>> list : handlers.values()) {
			List<Pair<Object, EventHandler>> toRemove = new ArrayList<>();
			for (Pair<Object, EventHandler> pair : list) {
				if (pair.left == listener)
					toRemove.add(pair);
			}
			list.removeAll(toRemove);
		}
	}
}
