package net.kodehawa.mantarobot.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class EasyReflection {
	@SuppressWarnings("unchecked")
	private static class Internal {
		public static Field field(Class c, String f) {
			try {
				return c.getDeclaredField(f);
			} catch (NoSuchFieldException e) {
				throw new RuntimeException(e);
			}
		}

		public static <R> R get(Field field, Object object, Class<R> returnType) {
			try {
				field.setAccessible(true);
				R r = (R) field.get(object);
				field.setAccessible(false);
				return r;
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		public static void set(Field field, Object object, Object set) {
			try {
				field.setAccessible(true);
				field.set(object, set);
				field.setAccessible(false);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}

		public static void setFinal(Field field, Object object, Object set) {
			set(field(Field.class, "modifiers"), field, field.getModifiers() & ~Modifier.FINAL);
			set(field, object, set);
		}
	}

	public static <T, R> Function<T, R> getObjectField(Class<T> clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return t -> Internal.get(field, t, type);
	}

	public static <R> Supplier<R> getStaticField(Class clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return () -> Internal.get(field, null, type);
	}

	public static <T, R> BiConsumer<T, R> setObjectField(Class<T> clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return (t, r) -> Internal.set(field, t, r);
	}

	public static <T, R> BiConsumer<T, R> setObjectFinalField(Class<T> clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return (t, r) -> Internal.set(field, t, r);
	}

	public static <R> Consumer<R> setStaticField(Class clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return r -> Internal.set(field, null, r);
	}

	public static <R> Consumer<R> setStaticFinalField(Class clazz, String fieldName, Class<R> type) {
		Field field = Internal.field(clazz, fieldName);
		return r -> Internal.set(field, null, r);
	}
}