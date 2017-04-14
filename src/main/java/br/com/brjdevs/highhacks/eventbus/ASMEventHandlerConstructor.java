package br.com.brjdevs.highhacks.eventbus;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.dynamic.scaffold.InstrumentedType;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;

import static net.bytebuddy.jar.asm.Opcodes.*;

/**
 * ASM bytecode appender of the constructor of an {@link ASMEventHandler}
 */
public class ASMEventHandlerConstructor implements ByteCodeAppender, Implementation {
	private final boolean isStatic;
	private final String thisClass;

	public ASMEventHandlerConstructor(String thisClass, boolean isStatic) {
		this.thisClass = thisClass.replace('.', '/');
		this.isStatic = isStatic;
	}

	@Override
	public ByteCodeAppender appender(Implementation.Target p1) {
		return this;
	}

	@Override
	public ByteCodeAppender.Size apply(MethodVisitor mv, Implementation.Context p2, MethodDescription p3) {
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "br/com/brjdevs/highhacks/eventbus/ASMEventHandler", "<init>", "()V", false);
		if (!isStatic) {
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitFieldInsn(PUTFIELD, thisClass, "instance", "Ljava/lang/Object;");
		}
		mv.visitInsn(RETURN);
		mv.visitMaxs(2, 2);
		return new ByteCodeAppender.Size(2, 2);
	}

	@Override
	public InstrumentedType prepare(InstrumentedType p1) {
		return p1;
	}
}
