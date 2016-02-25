package com.appiphany.nacc.utils;

import java.text.SimpleDateFormat;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.util.Log;

/**
 * A more natural android logging facility. WARNING: CHECK OUT COMMON PITFALLS
 * BELOW Unlike {@link android.util.Log}, Log provides sensible defaults. Debug
 * and Verbose logging is enabled for applications that have
 * "android:debuggable=true" in their AndroidManifest.xml. For apps built using
 * SDK Tools r8 or later, this means any debug build. Release builds built with
 * r8 or later will have verbose and debug log messages turned off. The default
 * tag is automatically set to your app's packagename, and the current context
 * (eg. activity, service, application, etc) is appended as well. You can add an
 * additional parameter to the tag using {@link #Log(String)}. Log-levels can be
 * programatically overridden for specific instances using
 * {@link #Log(String, boolean, boolean)}. Log messages may optionally use
 * {@link String#format(String, Object...)} formatting, which will not be
 * evaluated unless the log statement is output. Additional parameters to the
 * logging statement are treated as varrgs parameters to
 * {@link String#format(String, Object...)} Also, the current file and line is
 * automatically appended to the tag (this is only done if debug is enabled for
 * performance reasons). COMMON PITFALLS: * Make sure you put the exception
 * FIRST in the call. A common mistake is to place it last as is the
 * android.util.Log convention, but then it will get treated as varargs
 * parameter. * vararg parameters are not appended to the log message! You must
 * insert them into the log message using %s or another similar format parameter
 * Usage Examples: Ln.v("hello there"); Ln.d("%s %s", "hello", "there"); Ln.e(
 * exception, "Error during some operation"); Ln.w( exception,
 * "Error during %s operation", "some other");
 */
public final class Ln {
    /**
     * config is initially set to BaseConfig() with sensible defaults, then
     * replaced by BaseConfig(ContextSingleton) during guice static injection
     * pass.
     */
    private static final BaseConfig CONFIG = new BaseConfig();

    /**
     * print is initially set to Print(), then replaced by guice during static
     * injection pass. This allows overriding where the log message is delivered
     * to.
     */
    private static Print print = new Print();

    /**
     * Instantiates a new ln.
     */
    private Ln() {
    }

    /**
     * V.
     *
     * @param t the t
     * @return the int
     */
    public static int v(Throwable t) {
        return CONFIG.minimumLogLevel <= Log.VERBOSE ? print.println(Log.VERBOSE, Log.getStackTraceString(t)) : 0;
    }

    /**
     * V.
     *
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int v(Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.VERBOSE) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = args.length > 0 ? String.format(s, args) : s;
        return print.println(Log.VERBOSE, message);
    }

    /**
     * V.
     *
     * @param throwable the throwable
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int v(Throwable throwable, Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.VERBOSE) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = (args.length > 0 ? String.format(s, args) : s) + '\n' + Log.getStackTraceString(throwable);
        return print.println(Log.VERBOSE, message);
    }

    /**
     * D.
     *
     * @param t the t
     * @return the int
     */
    public static int d(Throwable t) {
        return CONFIG.minimumLogLevel <= Log.DEBUG ? print.println(Log.DEBUG, Log.getStackTraceString(t)) : 0;
    }

    /**
     * D.
     *
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int d(Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.DEBUG) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = args.length > 0 ? String.format(s, args) : s;
        return print.println(Log.DEBUG, message);
    }

    /**
     * D.
     *
     * @param throwable the throwable
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int d(Throwable throwable, Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.DEBUG) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = (args.length > 0 ? String.format(s, args) : s) + '\n' + Log.getStackTraceString(throwable);
        return print.println(Log.DEBUG, message);
    }

    /**
     * I.
     *
     * @param t the t
     * @return the int
     */
    public static int i(Throwable t) {
        return CONFIG.minimumLogLevel <= Log.INFO ? print.println(Log.INFO, Log.getStackTraceString(t)) : 0;
    }

    /**
     * I.
     *
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int i(Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.INFO) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = args.length > 0 ? String.format(s, args) : s;
        return print.println(Log.INFO, message);
    }

    /**
     * I.
     *
     * @param throwable the throwable
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int i(Throwable throwable, Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.INFO) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = (args.length > 0 ? String.format(s, args) : s) + '\n' + Log.getStackTraceString(throwable);
        return print.println(Log.INFO, message);
    }

    /**
     * W.
     *
     * @param t the t
     * @return the int
     */
    public static int w(Throwable t) {
        return CONFIG.minimumLogLevel <= Log.WARN ? print.println(Log.WARN, Log.getStackTraceString(t)) : 0;
    }

    /**
     * W.
     *
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int w(Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.WARN) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = args.length > 0 ? String.format(s, args) : s;
        return print.println(Log.WARN, message);
    }

    /**
     * W.
     *
     * @param throwable the throwable
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int w(Throwable throwable, Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.WARN) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = (args.length > 0 ? String.format(s, args) : s) + '\n' + Log.getStackTraceString(throwable);
        return print.println(Log.WARN, message);
    }

    /**
     * E.
     *
     * @param t the t
     * @return the int
     */
    public static int e(Throwable t) {
        return CONFIG.minimumLogLevel <= Log.ERROR ? print.println(Log.ERROR, Log.getStackTraceString(t)) : 0;
    }

    /**
     * E.
     *
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int e(Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.ERROR) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = args.length > 0 ? String.format(s, args) : s;
        return print.println(Log.ERROR, message);
    }

    /**
     * E.
     *
     * @param throwable the throwable
     * @param s1 the s1
     * @param args the args
     * @return the int
     */
    public static int e(Throwable throwable, Object s1, Object... args) {
        if (CONFIG.minimumLogLevel > Log.ERROR) {
            return 0;
        }

        final String s = Strings.toString(s1);
        final String message = (args.length > 0 ? String.format(s, args) : s) + '\n' + Log.getStackTraceString(throwable);
        return print.println(Log.ERROR, message);
    }

    /**
     * Checks if is debug enabled.
     *
     * @return true, if is debug enabled
     */
    public static boolean isDebugEnabled() {
        return CONFIG.minimumLogLevel <= Log.DEBUG;
    }

    /**
     * Checks if is verbose enabled.
     *
     * @return true, if is verbose enabled
     */
    public static boolean isVerboseEnabled() {
        return CONFIG.minimumLogLevel <= Log.VERBOSE;
    }

    /**
     * Gets the config.
     *
     * @return the config
     */
    public static Config getConfig() {

        return CONFIG;
    }

    /**
     * The Interface Config.
     */
    public interface Config {
        
        /**
         * Gets the logging level.
         *
         * @return the logging level
         */
        int getLoggingLevel();

        /**
         * Sets the logging level.
         *
         * @param level the new logging level
         */
        void setLoggingLevel(int level);
    }

    /**
     * The Class BaseConfig.
     */
    public static class BaseConfig implements Config {
        
        /** The minimum log level. */
        protected int minimumLogLevel = Log.VERBOSE;
        
        /** The package name. */
        protected String packageName = "";
        
        /** The scope. */
        protected String scope = "";

        /**
         * Instantiates a new base config.
         */
        protected BaseConfig() {
        }

        /**
         * Instantiates a new base config.
         *
         * @param context the context
         */
        public BaseConfig(Application context) {
            try {
                packageName = context.getPackageName();
                final int flags = context.getPackageManager().getApplicationInfo(packageName, 0).flags;
                minimumLogLevel = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? Log.VERBOSE : Log.INFO;
                scope = packageName.toUpperCase();
                Ln.d("Configuring Logging, minimum log level is %s", logLevelToString(minimumLogLevel));
            } catch (Exception e) {
                Log.e(packageName, "Error configuring logger", e);
            }
        }

        /* (non-Javadoc)
         * @see com.org.scgroup.utils.Ln.Config#getLoggingLevel()
         */
        @Override
        public int getLoggingLevel() {
            return minimumLogLevel;
        }

        /* (non-Javadoc)
         * @see com.org.scgroup.utils.Ln.Config#setLoggingLevel(int)
         */
        @Override
        public void setLoggingLevel(int level) {
            minimumLogLevel = level;
        }
    }

    /**
     * Log level to string.
     *
     * @param loglevel the loglevel
     * @return the string
     */
    public static String logLevelToString(int loglevel) {
        switch (loglevel) {
            case Log.VERBOSE:
                return "VERBOSE";
            case Log.DEBUG:
                return "DEBUG";
            case Log.INFO:
                return "INFO";
            case Log.WARN:
                return "WARN";
            case Log.ERROR:
                return "ERROR";
            case Log.ASSERT:
                return "ASSERT";

            default:
                return "UNKNOWN";
        }

    }

    /**
     * Sets the prints the.
     *
     * @param print the new prints the
     */
    public static void setPrint(Print print) {
        Ln.print = print;
    }

    /** Default implementation logs to android.util.Log */
    public static class Print {
        
        /** The Constant DEFAULT_STACK_TRACE_LINE_COUNT. */
        private static final int DEFAULT_STACK_TRACE_LINE_COUNT = 5;

        /**
         * Println.
         *
         * @param priority the priority
         * @param msg the msg
         * @return the int
         */
        public int println(int priority, String msg) {
            return Log.println(priority, getScope(), processMessage(msg));
        }

        /**
         * Process message.
         *
         * @param msg the msg
         * @return the string
         */
        protected String processMessage(String msg) {
            if (CONFIG.minimumLogLevel <= Log.DEBUG) {
                msg = String.format("%s %s %s", new SimpleDateFormat("HH:mm:ss.SSS").format(System.currentTimeMillis()), Thread.currentThread().getName(), msg);
            }
            return msg;
        }

        /**
         * Gets the scope.
         *
         * @return the scope
         */
        protected static String getScope() {
            if (CONFIG.minimumLogLevel <= Log.DEBUG) {
                final StackTraceElement trace = Thread.currentThread().getStackTrace()[DEFAULT_STACK_TRACE_LINE_COUNT];
                return CONFIG.scope + "/" + trace.getFileName() + ":" + trace.getLineNumber();
            }

            return CONFIG.scope;
        }

    }
}
