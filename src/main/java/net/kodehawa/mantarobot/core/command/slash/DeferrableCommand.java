package net.kodehawa.mantarobot.core.command.slash;

import net.kodehawa.mantarobot.core.command.AnnotatedCommand;
import net.kodehawa.mantarobot.core.command.meta.Defer;
import net.kodehawa.mantarobot.core.command.meta.Ephemeral;
import net.kodehawa.mantarobot.core.command.meta.ModalInteraction;

public abstract class DeferrableCommand<T extends IContext> extends AnnotatedCommand<T> {
    protected boolean defer;
    protected boolean ephemeral;
    protected final boolean modal;

    public DeferrableCommand() {
        var clazz = getClass();

        this.defer = clazz.getAnnotation(Defer.class) != null;
        this.ephemeral = clazz.getAnnotation(Ephemeral.class) != null;
        this.modal = clazz.getAnnotation(ModalInteraction.class) != null;
    }

    @SuppressWarnings("unused")
    public void setEphemeral(boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    @SuppressWarnings("unused")
    public void setDefer(boolean defer) {
        this.defer = defer;
    }

    public boolean defer() {
        return defer;
    }
}
