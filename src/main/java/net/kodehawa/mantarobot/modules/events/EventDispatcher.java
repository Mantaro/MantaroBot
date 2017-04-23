package net.kodehawa.mantarobot.modules.events;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class EventDispatcher {
	public static Map<Class<?>, Object> instances = new HashMap<>();

	public static void dispatch(Set<Method> methods, Object event) {
		methods.stream().filter(method -> method.getParameterCount() == 1 && event.getClass().isInstance(method.getParameterTypes()[0]))
			.forEach(method -> {
				try {
					Object instance = null;
					if (!Modifier.isStatic(method.getModifiers())) {
						instance = instances.get(method.getDeclaringClass());

						if (instance == null) {
							instance = method.getDeclaringClass().newInstance();
							instances.put(method.getDeclaringClass(), instance);
						}
					}

					method.invoke(instance, event);
				} catch (InstantiationException | InvocationTargetException e) {
					log.error("[Developer Error] Could not initialize a command.", e);
				} catch (IllegalAccessException e) {
					log.error("[Developer Error] Could not access a command class!", e);
				}
			});
	}
}
