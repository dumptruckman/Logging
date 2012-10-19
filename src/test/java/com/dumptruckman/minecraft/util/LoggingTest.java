package com.dumptruckman.minecraft.util;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.MockGateway;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.MissingFormatArgumentException;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PluginDescriptionFile.class)
public class LoggingTest {

    static final String NAME = "Logging-Test";
    static final String VERSION = "1.2.3-test";

    static final String SIMPLE_MESSAGE = "This is a test.";
    static final String ARGS_MESSAGE = "This is a %s test with some %s%s";

    Plugin plugin;

    @Before
    public void setUp() throws Exception {
        MockGateway.MOCK_STANDARD_METHODS = false;
        plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("Logging-Test");
        final PluginDescriptionFile pdf = PowerMockito.spy(new PluginDescriptionFile(NAME, VERSION,
                "com.dumptruckman.minecraft.util.LoggingTest"));
        when(plugin.getDescription()).thenReturn(pdf);
        FileUtils.deleteFolder(new File("bin"));
        final File testFolder = new File("bin/test/server/plugins/Logging-Test");
        testFolder.mkdirs();
        when(plugin.getDataFolder()).thenReturn(testFolder);
        Logging.init(plugin);
    }

    @After
    public void tearDown() throws Exception {
        //FileUtils.deleteFolder(new File("bin"));
    }

    @Test
    public void testInit() throws Exception {
        assertEquals(Logging.name, plugin.getName());
        assertEquals(Logging.version, plugin.getDescription().getVersion());
        assertEquals(Logging.debugLevel, Logging.ORIGINAL_DEBUG_LEVEL);
        assertEquals(Logging.plugin, plugin);
    }

    @Test
    public void testShutdown() throws Exception {
        Logging.shutdown();
        assertEquals(Logging.name, Logging.ORIGINAL_NAME);
        assertEquals(Logging.version, Logging.ORIGINAL_VERSION);
        assertEquals(Logging.debugLevel, Logging.ORIGINAL_DEBUG_LEVEL);
        assertEquals(Logging.debug, Logging.ORIGINAL_DEBUG);
        assertNull(Logging.plugin);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDebugLevelTooHigh() throws Exception {
        Logging.setDebugLevel(4);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDebugLevelTooLow() throws Exception {
        Logging.setDebugLevel(-1);
    }

    @Test
    public void testSetDebugLevel() throws Exception {
        assertNull(Logging.debugLog);
        assertTrue(DebugLog.isClosed());
        Logging.setDebugLevel(1);
        assertFalse(DebugLog.isClosed());
        assertEquals(DebugLog.getLoggerName(), plugin.getName());
        assertEquals(DebugLog.getFileName(), Logging.getDebugFileName(plugin));
        assertNotNull(Logging.debugLog);
        final DebugLog debugLog = Logging.debugLog;
        assertSame(DebugLog.getDebugLogger(), debugLog);
        Logging.setDebugLevel(2);
        assertSame(debugLog, Logging.debugLog);
        Logging.setDebugLevel(3);
        assertSame(debugLog, Logging.debugLog);
        Logging.setDebugLevel(0);
        assertNull(Logging.debugLog);
        assertTrue(DebugLog.isClosed());
    }

    @Test
    public void testGetDebugLevel() throws Exception {
        assertEquals(Logging.getDebugLevel(), 0);
        for (int i = 3; i >= 0; i--) {
            Logging.setDebugLevel(i);
            assertEquals(Logging.getDebugLevel(), i);
        }
    }

    @Test
    public void testCloseDebugLog() throws Exception {
        Logging.setDebugLevel(3);
        assertFalse(DebugLog.isClosed());
        Logging.closeDebugLog();
        assertTrue(DebugLog.isClosed());
    }

    @Test
    public void testGetPrefixedMessage() throws Exception {
        assertEquals("[" + NAME + "] " + SIMPLE_MESSAGE, Logging.getPrefixedMessage(SIMPLE_MESSAGE, false));
        assertEquals("[" + NAME + " " + VERSION + "] " + SIMPLE_MESSAGE, Logging.getPrefixedMessage(SIMPLE_MESSAGE, true));
    }

    @Test
    public void testSetGetDebugPrefix() throws Exception {
        final String debugging = "-debugging";
        assertEquals("[" + NAME + Logging.ORIGINAL_DEBUG + "] " + SIMPLE_MESSAGE, Logging.getDebugString(SIMPLE_MESSAGE));
        Logging.setDebugPrefix(debugging);
        assertEquals("[" + NAME + debugging + "] " + SIMPLE_MESSAGE, Logging.getDebugString(SIMPLE_MESSAGE));
        Logging.init(plugin);
        assertEquals("[" + NAME + Logging.ORIGINAL_DEBUG + "] " + SIMPLE_MESSAGE, Logging.getDebugString(SIMPLE_MESSAGE));
    }

    static class TestHandler extends Handler {

        Level level;
        RecordTester tester;

        @Override
        public void publish(LogRecord record) {
            assertEquals(Logging.getLogger().getName(), record.getLoggerName());
            assertEquals(level, record.getLevel());
            tester.test(record);
        }

        @Override
        public void flush() { }

        @Override
        public void close() throws SecurityException { }
    }

    static abstract class RecordTester {
        public abstract void test(final LogRecord record);
    }

    @Test(expected = MissingFormatArgumentException.class)
    public void testLog() throws Exception {
        Logging.setDebugLevel(3);
        final TestHandler handler = new TestHandler();
        Logging.getLogger().addHandler(handler);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getPrefixedMessage(SIMPLE_MESSAGE, false), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        Logging.log(false, Level.INFO, SIMPLE_MESSAGE);
        handler.level = Level.WARNING;
        Logging.log(false, Level.WARNING, SIMPLE_MESSAGE);
        handler.level = Level.SEVERE;
        Logging.log(false, Level.SEVERE, SIMPLE_MESSAGE);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getPrefixedMessage(SIMPLE_MESSAGE, true), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        Logging.log(true, Level.INFO, SIMPLE_MESSAGE);
        handler.level = Level.WARNING;
        Logging.log(true, Level.WARNING, SIMPLE_MESSAGE);
        handler.level = Level.SEVERE;
        Logging.log(true, Level.SEVERE, SIMPLE_MESSAGE);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getDebugString(SIMPLE_MESSAGE), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        Logging.log(false, Level.FINE, SIMPLE_MESSAGE);
        Logging.log(false, Level.FINER, SIMPLE_MESSAGE);
        Logging.log(false, Level.FINEST, SIMPLE_MESSAGE);
        Logging.log(true, Level.FINE, SIMPLE_MESSAGE);
        Logging.log(true, Level.FINER, SIMPLE_MESSAGE);
        Logging.log(true, Level.FINEST, SIMPLE_MESSAGE);

        Logging.getLogger().removeHandler(handler);

        final Object o = new Object();
        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getPrefixedMessage(String.format(ARGS_MESSAGE, "poop", 2, o), false), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        Logging.log(false, Level.INFO, ARGS_MESSAGE, "poop", 2, o);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getPrefixedMessage(String.format(ARGS_MESSAGE, "poop", 2, o), false), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        Logging.log(false, Level.INFO, ARGS_MESSAGE, "poop", 2);
    }

    @Test
    public void testGetLogger() throws Exception {
        Logging.setDebugLevel(3);
        final TestHandler handler = new TestHandler();
        final Logger logger = Logging.getLogger();
        logger.addHandler(handler);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getPrefixedMessage(SIMPLE_MESSAGE, false), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        logger.log(Level.INFO, SIMPLE_MESSAGE);
        handler.level = Level.WARNING;
        logger.log(Level.WARNING, SIMPLE_MESSAGE);
        handler.level = Level.SEVERE;
        logger.log(Level.SEVERE, SIMPLE_MESSAGE);

        handler.tester = new RecordTester() {
            @Override
            public void test(LogRecord record) {
                assertEquals(Logging.getDebugString(SIMPLE_MESSAGE), record.getMessage());
            }
        };
        handler.level = Level.INFO;
        logger.log(Level.FINE, SIMPLE_MESSAGE);
        logger.log(Level.FINER, SIMPLE_MESSAGE);
        logger.log(Level.FINEST, SIMPLE_MESSAGE);

        Logging.getLogger().removeHandler(handler);
    }
}
