package net.kodehawa.mantarobot.utils;

import sun.misc.Unsafe;
import sun.reflect.CallerSensitive;

import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;

public class UnsafeUtils {
    private static final Unsafe unsafe;

    static {
        try {
            Constructor<Unsafe> ctor = Unsafe.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            unsafe = ctor.newInstance();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocate(Class<T> klass) {
        try {
           return (T)unsafe.allocateInstance(klass);
       } catch (InstantiationException e) {
           return null;
       }
    }

    public static void initializeClass(Class<?> klass) {
        unsafe.ensureClassInitialized(klass);
    }

    public static void throwException(Throwable t) {
        unsafe.throwException(t);
    }

    @CallerSensitive
    public static Class<?> defineClass(String name, byte[] bytes, ClassLoader loader) {
        return defineClass(name, bytes, 0, bytes.length, loader, getCallingClass().getProtectionDomain());
    }

    public static Class<?> defineClass(String name, byte[] bytes, int offset, int length, ClassLoader loader, ProtectionDomain domain) {
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
            throw new InternalError("Shouldn't ever happen");
        }
    }
}
