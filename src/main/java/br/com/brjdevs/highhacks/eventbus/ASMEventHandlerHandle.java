package br.com.brjdevs.highhacks.eventbus;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Type;

import java.lang.reflect.Method;

import static net.bytebuddy.jar.asm.Opcodes.*;

/**
 * ASM bytecode appender of the {@link EventHandler#handle(Object)} method of an {@link ASMEventHandler}
 */
public class ASMEventHandlerHandle implements ByteCodeAppender, Implementation {
	private final Method callback;
	private final String eventClass;
	private final boolean isStatic;
	private final String listenerClass;
	private final String thisClass;

	public ASMEventHandlerHandle(String thisClass, String listenerClass, String eventClass, Method callback, boolean isStatic) {
		this.thisClass = thisClass.replace('.', '/');
		this.listenerClass = listenerClass.replace('.', '/');
		this.eventClass = eventClass.replace('.', '/');
		this.callback = callback;
		this.isStatic = isStatic;
	}

	@Override
	public ByteCodeAppender appender(Implementation.Target p1) {
		return this;
	}

	@Override
	public ByteCodeAppender.Size apply(MethodVisitor mv, Implementation.Context implementationContext, MethodDescription instrumentedMethod) {
		mv.visitVarInsn(ALOAD, 0);
		if (!isStatic) {
			mv.visitFieldInsn(GETFIELD, thisClass, "instance", "Ljava/lang/Object;");
			mv.visitTypeInsn(CHECKCAST, listenerClass);
		}
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, eventClass);
		mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, listenerClass, callback.getName(), Type.getMethodDescriptor(callback), false);
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		return new Size(2, 2);
	}

	@Override
	public InstrumentedType prepare(InstrumentedType p1) {
		return p1;
	}
}
