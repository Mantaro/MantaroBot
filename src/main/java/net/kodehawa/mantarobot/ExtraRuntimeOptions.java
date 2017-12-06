package net.kodehawa.mantarobot;

public class ExtraRuntimeOptions {
    public static final boolean DEBUG = System.getProperty("mantaro.debug") != null;
    public static final boolean VERBOSE = System.getProperty("mantaro.verbose") != null;

    public static final boolean DEBUG_LOGS = System.getProperty("mantaro.debug_logs") != null;
    public static final boolean TRACE_LOGS = System.getProperty("mantaro.trace_logs") != null;

    public static final boolean LOG_DB_ACCESS = System.getProperty("mantaro.log_db_access") != null;
    public static final boolean LOG_CACHE_ACCESS = System.getProperty("mantaro.log_cache_access") != null;
}
