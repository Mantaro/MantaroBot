package net.kodehawa.mantarobot.utils;

import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;

import java.lang.reflect.*;
import java.security.ProtectionDomain;

public class UnsafeUtils {
	private static final Method defineClass;
	private static final Field modifiersField;
	private static final Unsafe unsafe;

	static {
		Unsafe u;
		try {
			Constructor<Unsafe> ctor = Unsafe.class.getDeclaredConstructor();
			ctor.setAccessible(true);
			u = ctor.newInstance();
		} catch (Exception e) {
			u = null;
		}
		unsafe = u;
		Method d;
		try {
			d = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
			d.setAccessible(true);
		} catch (NoSuchMethodException e) {
			d = null;
		}
		defineClass = d;
		Field f;
		try {
			f = Field.class.getDeclaredField("modifiers");
			f.setAccessible(true);
		} catch (NoSuchFieldException e) {
			f = null;
		}
		modifiersField = f;
	}

	@SuppressWarnings("unchecked")
	public static <T> T allocate(Class<T> klass) {
		try {
			return (T) unsafe.allocateInstance(klass);
		} catch (InstantiationException e) {
			return null;
		}
	}

	@CallerSensitive
	public static Class<?> defineClass(String name, byte[] bytes, ClassLoader loader) {
		return defineClass(name, bytes, 0, bytes.length, loader, getCallingClass().getProtectionDomain());
	}

	public static Class<?> defineClass(String name, byte[] bytes, int offset, int length, ClassLoader loader, ProtectionDomain domain) {
		if (unsafe == null) {
			if (defineClass == null)
				return null;
			try {
				return (Class<?>) defineClass.invoke(loader, name, bytes, offset, length, domain);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof ClassFormatError)
					throw (ClassFormatError) cause;
				throw new InternalError(cause);
			} catch (IllegalAccessException e) {
				throw new AssertionError();
			}
		}
		return unsafe.defineClass(name, bytes, offset, length, loader, domain);
	}

	public static Class<?> getCallingClass() {
		return getCallingClass(3);
	}

	public static Class<?> getCallingClass(int depth) {
		StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
		try {
			return Class.forName(stacktrace[depth].getClassName());
		} catch (ClassNotFoundException e) {
			throw new AssertionError();
		}
	}

	public static Unsafe getUnsafe() {
		return unsafe;
	}

	public static void initializeClass(Class<?> klass) {
		if (unsafe == null) {
			try {
				Class.forName(klass.getName(), true, klass.getClassLoader());
			} catch (ClassNotFoundException e) {
				throw new AssertionError();
			}
			return;
		}
		unsafe.ensureClassInitialized(klass);
	}

	public static void throwException(Throwable t) {
		if (unsafe == null) throw new RuntimeException(t);
		unsafe.throwException(t);
	}

	public static void updateStaticFinalField(Field field, Object value) {
		updateStaticFinalField(field, value, false);
	}

	public static void updateStaticFinalField(Field field, Object value, boolean volatileSet) {
		if (field == null) throw new NullPointerException("field");
		if (!Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException("Field not static");
		if (field.getType().isPrimitive())
			throw new IllegalArgumentException("Primitive fields cannot be updated"); //inlined by the compiler (would have no effect)
		if (field.getType() == String.class)
			throw new IllegalArgumentException("String fields cannot be updated"); //inlined too
		if (unsafe == null) {
			if (modifiersField == null)
				throw new UnsupportedOperationException("Neither unsafe nor reflection methods available");
			try {
				modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
			} catch (IllegalAccessException impossible) {
				throw new AssertionError(impossible);
			}
			field.setAccessible(true);
			try {
				field.set(null, value);
			} catch (IllegalAccessException impossible) {
				throw new AssertionError(impossible);
			}
			return;
		}
		if (volatileSet)
			unsafe.putObjectVolatile(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
		else
			unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
	}
}
