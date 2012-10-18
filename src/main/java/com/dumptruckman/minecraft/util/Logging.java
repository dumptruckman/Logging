/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package com.dumptruckman.minecraft.util;

import com.onarandombox.util.DebugLog;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static plugin logger.
 */
public class Logging extends Logger {

    static final String ORIGINAL_NAME = Logging.class.getSimpleName();
    static final String ORIGINAL_VERSION = "v.???";
    static final String ORIGINAL_DEBUG = "-Debug";
    static final int ORIGINAL_DEBUG_LEVEL = 0;

    static final Logging LOG = new Logging(Logger.getLogger("Minecraft"));

    static String name = ORIGINAL_NAME;
    static String version = ORIGINAL_VERSION;
    static String debug = ORIGINAL_DEBUG;
    static int debugLevel = ORIGINAL_DEBUG_LEVEL;
    static DebugLog debugLog = null;
    static Plugin plugin = null;

    protected Logging(final Logger logger) {
        super(logger.getName(), logger.getResourceBundleName());
    }

    /**
     * Prepares the log for use.  Debugging will default to disabled when initialized.  This should be called early on
     * in plugin initialization, such as during onLoad() or onEnable().  If this {@link Logging} class has already
     * been initialized, it will first be shut down before reinitializing.
     *
     * @param plugin The plugin using this static logger.
     */
    public static void init(final Plugin plugin) {
        if (Logging.plugin != null) {
            shutdown();
        }
        name = plugin.getName();
        version = plugin.getDescription().getVersion();
        DebugLog.init(name, plugin.getDataFolder() + File.separator + "debug.log");
        setDebugLevel(0);
        Logging.plugin = plugin;
    }

    /**
     * Returns the {@link Logging} class to it's original state, releasing the plugin that initialized it.  The
     * {@link Logging} class can be reinitialized once it has been shut down.  This should be called when the plugin
     * is disabled so that a static reference to the plugin is not kept in cases of server reloads.
     */
    public static void shutdown() {
        closeDebugLog();
        DebugLog.shutdown();
        plugin = null;
        name = ORIGINAL_NAME;
        version = ORIGINAL_VERSION;
        debug = ORIGINAL_DEBUG;
        debugLevel = ORIGINAL_DEBUG_LEVEL;
    }

    /**
     * Closes the debug log if it is open.
     */
    public static void closeDebugLog() {
        if (debugLog != null) {
            debugLog.close();
            debugLog = null;
        }
    }

    /**
     * Sets the debug logging level of this plugin.  Debug messages will print to the console and to a
     * debug log file when enabled.
     * debugLevel:
     *   0 - turns off debug logging, disabling the debug logger, closing any open file hooks.
     *   1 - enables debug logging of {@link java.util.logging.Level#FINE} or lower messages.
     *   2 - enables debug logging of {@link java.util.logging.Level#FINER} or lower messages.
     *   3 - enables debug logging of {@link java.util.logging.Level#FINEST} or lower messages.
     *
     * @param debugLevel 0 = off, 1-3 = debug level
     */
    public static void setDebugLevel(final int debugLevel) {
        if (debugLevel > 3 || debugLevel < 0) {
            throw new IllegalArgumentException("debugLevel must be between 0 and 3!");
        }
        if (debugLevel > 0) {
            debugLog = DebugLog.getDebugLogger();
        } else {
            closeDebugLog();
        }
        Logging.debugLevel = debugLevel;
    }

    /**
     * Returns the current debug logging level.
     *
     * @return A value 0-3 indicating the debug logging level.
     */
    public static int getDebugLevel() {
        return debugLevel;
    }

    /**
     * Adds the plugin name and optionally the version number to the log message.
     *
     * @param message     Log message
     * @param showVersion Whether to show version in log message
     * @return Modified message
     */
    public static String getPrefixedMessage(final String message, final boolean showVersion) {
        final StringBuilder builder = new StringBuilder("[").append(name);
        if (showVersion) {
            builder.append(" ").append(version);
        }
        builder.append("] ").append(message);
        return builder.toString();
    }

    /**
     * Sets the debug prefix for debug messages that follows the plugin name.  The default is "-Debug".
     *
     * @param debugPrefix the new debug prefix to use.
     */
    public static void setDebugPrefix(final String debugPrefix) {
        Logging.debug = debugPrefix;
    }

    /**
     * Adds the plugin's debug name to the message.
     *
     * @param message     Log message
     * @return Modified message
     */
    public static String getDebugString(final String message) {
        return "[" + name + debug + "] " + message;
    }

    /**
     * Returns the static Logger instance used by this class.
     *
     * @return the static Logger instance used by this class.
     */
    public static Logging getLogger() {
        return LOG;
    }

    /**
     * Custom log method.  Always logs to a single static logger.  Applies String.format() to the message if it is a
     * non-debug level logging and to debug level logging IF debug logging is enabled.  Optionally appends version to
     * prefix.
     *
     * @param showVersion True adds version into message prefix.
     * @param level       One of the message level identifiers, e.g. SEVERE.
     * @param message     The string message.
     * @param args        Arguments for the String.format() that is applied to the message.
     */
    public static void log(final boolean showVersion, final Level level, String message, final Object... args) {
        if ((level == Level.FINE && Logging.debugLevel >= 1)
                || (level == Level.FINER && Logging.debugLevel >= 2)
                || (level == Level.FINEST && Logging.debugLevel >= 3)) {
            debug(Level.INFO, message, args);
        } else if (level != Level.FINE && level != Level.FINER && level != Level.FINEST) {
            LOG._log(level, getPrefixedMessage(String.format(message, args), showVersion));
        }
    }

    /**
     * Custom log method.  Always logs to a single static logger.  Applies String.format() to the message if it is a
     * non-debug level logging and to debug level logging IF debug logging is enabled.  Does not append version to
     * prefix.
     *
     * @param level       One of the message level identifiers, e.g. SEVERE.
     * @param message     The string message.
     * @param args        Arguments for the String.format() that is applied to the message.
     */
    public static void logStatic(final Level level, String message, final Object... args) {
        log(false, level, message, args);
    }

    void _log(final Level level, final String message) {
        super.log(level, message);
        if (debugLog != null) {
            debugLog.log(level, message);
        }
    }

    /**
     * Log a message, with no arguments.  Similar to {@link Logger#log(java.util.logging.Level, String)} with the
     * exception that all logging is handled by a single static {@link Logging} instance.
     *
     * If the logger is currently enabled for the given message level then the given message is forwarded to all the
     * registered output Handler objects.
     *
     * @param level Log level
     * @param message Log message
     */
    @Override
    public void log(final Level level, final String message) {
        if ((level == Level.FINE && Logging.debugLevel >= 1)
                || (level == Level.FINER && Logging.debugLevel >= 2)
                || (level == Level.FINEST && Logging.debugLevel >= 3)) {
            LOG._log(Level.INFO, getDebugString(message));
        } else if (level != Level.FINE && level != Level.FINER && level != Level.FINEST) {
            LOG._log(level, message);
        }
    }

    /**
     * Directly outputs a message with the debug prefix to both the regular logger and the debug logger if one is set.
     *
     * @param message The message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    static void debug(final Level level, String message, final Object...args) {
        LOG._log(level, getDebugString(String.format(message, args)));
    }

    /**
     * Fine debug level logging.  Use for infrequent messages.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void fine(final String message, final Object...args) {
        Logging.log(false, Level.FINE, message, args);
    }

    /**
     * Finer debug level logging.  Use for somewhat frequent messages.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void finer(final String message, final Object...args) {
        Logging.log(false, Level.FINER, message, args);
    }

    /**
     * Finest debug level logging.  Use for extremely frequent messages.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void finest(final String message, final Object...args) {
        Logging.log(false, Level.FINEST, message, args);
    }

    /**
     * Info level logging.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void info(final String message, final Object...args) {
        Logging.log(false, Level.INFO, message, args);
    }

    /**
     * Warning level logging.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void warning(final String message, final Object...args) {
        Logging.log(false, Level.WARNING, message, args);
    }

    /**
     * Severe level logging.
     *
     * @param message Message to log.
     * @param args    Arguments for the String.format() that is applied to the message.
     */
    public static void severe(final String message, final Object...args) {
        Logging.log(false, Level.SEVERE, message, args);
    }

}


