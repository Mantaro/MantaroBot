package net.kodehawa.mantarobot.connectionwatcher;

import net.sandius.rembulan.StateContext;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.Variable;
import net.sandius.rembulan.compiler.CompilerChunkLoader;
import net.sandius.rembulan.env.RuntimeEnvironments;
import net.sandius.rembulan.exec.CallException;
import net.sandius.rembulan.exec.DirectCallExecutor;
import net.sandius.rembulan.impl.ImmutableTable;
import net.sandius.rembulan.impl.NonsuspendableFunctionException;
import net.sandius.rembulan.impl.StateContexts;
import net.sandius.rembulan.lib.StandardLibrary;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.AbstractFunctionAnyArg;
import net.sandius.rembulan.runtime.ExecutionContext;
import net.sandius.rembulan.runtime.ResolvedControlThrowable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

public class LuaEvaluator {
    private static final StateContext state = StateContexts.newDefaultInstance();
    private static final Table lib;

    static {
        lib = StandardLibrary.in(RuntimeEnvironments.system()).installInto(state);

        ImmutableTable.Builder builder = new ImmutableTable.Builder();
        builder.add("getReboots", new LuaFunction(args->ConnectionWatcher.getInstance().getReboots()));
        builder.add("reboot", new LuaFunction(args-> {
            ConnectionWatcher.getInstance().launchMantaro(args.length > 0 && Boolean.TRUE.equals(args[0]));
            return null;
        }));
        builder.add("stop", new LuaFunction(args->{
            ConnectionWatcher.getInstance().stopMantaro(args.length > 0 && Boolean.TRUE.equals(args[0]));
            return null;
        }));
        builder.add("exit", new LuaFunction(args->{
            ConnectionWatcher.getInstance().stopMantaro(args.length > 0 && Boolean.TRUE.equals(args[0]));
            ConnectionWatcher.getInstance().jda.shutdown();
            System.exit(0);
            return null;
        }));
        builder.add("getOwners", new LuaFunction(args->ConnectionWatcher.getInstance().getOwners()));
        builder.add("getJvmArgs", new LuaFunction(args->ConnectionWatcher.getInstance().getJvmArgs()));
        builder.add("getJdaPing", new LuaFunction(args->ConnectionWatcher.getInstance().jda.getPing()));
        builder.add("log", new LuaFunction(args->{
            for(Object o : args)
                ConnectionWatcher.LOGGER.info(String.valueOf(o));
            return null;
        }));
        lib.rawset("cw", builder.build());
    }

    public static Object[] eval(String code) throws LoaderException, RunningException {
        CompilerChunkLoader loader = CompilerChunkLoader.of(new ClassLoader(LuaEvaluator.class.getClassLoader()){}, "lua_code");
        try {
            return DirectCallExecutor.newExecutor().call(state, loader
                    .loadTextChunk(new Variable(lib), "main", code));
        } catch(LoaderException e) {
            throw e;
        } catch(CallException e) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
            e.printLuaFormatStackTraceback(ps, loader.getChunkClassLoader(), new String[0]);
            ps.close();
            throw new RunningException(new String(baos.toByteArray(), Charset.defaultCharset()));
        } catch(Exception e) {
            ConnectionWatcher.LOGGER.error("Error evaluating", e);
            return null;
        }
    }

    public static class RunningException extends Exception {
        private final String traceback;

        private RunningException(String traceback) {
            super("Error running");
            this.traceback = traceback;
        }

        public String getTraceback() {
            return traceback;
        }
    }

    public static class LuaFunction extends AbstractFunctionAnyArg {
        private final Function<Object[], Object> function;

        private LuaFunction(Function<Object[], Object> function) {
            this.function = function;
        }

        @Override
        public void invoke(ExecutionContext context, Object[] args) throws ResolvedControlThrowable {
            try {
                context.getReturnBuffer().setTo(function.apply(args));
            } catch(Exception e) {
                context.getReturnBuffer().setTo(null, e.toString());
            }
        }

        @Override
        public void resume(ExecutionContext context, Object suspendedState) throws ResolvedControlThrowable {
            throw new NonsuspendableFunctionException();
        }
    }

    @FunctionalInterface
    private interface Function<A, R> {
        R apply(A a) throws Exception;
    }
}
