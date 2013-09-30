package de.apaxo.test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import de.apaxo.test.CachingSpy.FileNameConstructor;

import static de.apaxo.test.CachingSpy.cachingSpy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * This is a test case for the CachingSpy.
 * 
 * @author Manue Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
public class CachingSpyTest {

    private static final Logger log = Logger.getLogger(CachingSpyTest.class
                                            .getName());

    @Before
    public void configureLogging() {
        try {
            System.setProperty("java.util.logging.config.file",
                    new File(getClass().getClassLoader().getResource("")
                            .toURI()).getPath()
                            + "/logging.properties");

            LogManager.getLogManager().readConfiguration();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCachingSpyT() {
        TestObject testObject = new TestObject();
        assertEquals("Hallo Welt0", testObject.echo("Hallo Welt"));
        assertEquals("Hallo Welt1", testObject.echo("Hallo Welt"));
        testObject = cachingSpy(testObject);
        assertEquals("Hallo Welt2", testObject.echo("Hallo Welt"));
        assertEquals("Hallo Welt2", testObject.echo("Hallo Welt"));
        assertNotNull(getClass().getClassLoader().getResource(
                "de.apaxo.test.TestObject-echo--972308545.dat"));
    }

    @Test
    public void testCachingWithGivenFile() {
        TestObject testObject = new TestObject();
        testObject = cachingSpy(testObject);
        // this is read from de.apaxo.test.TestObject-five-1.dat
        // which is a copy of de.apaxo.test.TestObject-four-1.dat
        assertEquals(4, testObject.five());
        assertEquals(4, testObject.four());
    }

    @Test
    public void testCachingSpyTMethodArray() {
        TestObject testObject = new TestObject();
        assertEquals("Hallo Welt0", testObject.echo("Hallo Welt"));
        assertEquals("Hallo Welt1", testObject.echo("Hallo Welt"));
        // do not spy on any methods
        testObject = cachingSpy(testObject, new Method[] {});
        assertEquals("Hallo Welt2", testObject.echo("Hallo Welt"));
        assertEquals("Hallo Welt3", testObject.echo("Hallo Welt"));
        // do not spy on any methods
        try {
            try {
                testObject = cachingSpy(testObject, new Method[] { testObject
                        .getClass().getDeclaredMethod("echo", String.class) });
            } catch (IllegalArgumentException ex) {
                assertEquals("The object that you supplied " + testObject
                        + " is already a mock." + " Please create a new one.",
                        ex.getMessage());
            }
            testObject = new TestObject();
            assertEquals("Hallo Welt0", testObject.echo("Hallo Welt"));
            assertEquals("Hallo Welt1", testObject.echo("Hallo Welt"));
            testObject = cachingSpy(testObject, new Method[] { testObject
                    .getClass().getDeclaredMethod("echo", String.class) });
            assertEquals("Hallo Welt2", testObject.echo("Hallo Welt"));
            assertEquals("Hallo Welt2", testObject.echo("Hallo Welt"));
        } catch (NoSuchMethodException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
            fail();
        } catch (SecurityException e) {
            log.log(Level.WARNING, "Exception was thrown", e);
            fail();
        }
        assertNotNull(getClass().getClassLoader().getResource(
                "de.apaxo.test.TestObject-echo--972308545.dat"));
    }

    @Test
    public void testFileNaming() throws NoSuchMethodException, SecurityException {
        TestObject testObject = new TestObject();
        CachingSpy.addFileNameConstructor(testObject.getClass().getMethod("fileNameAsParameter", new Class<?>[] {String.class}), new FileNameConstructor() {
            
            @Override
            public String generateFileName(Object[] arguments) {
                // directly return the first parameter as string
                return arguments[0] == null ? "null" : arguments[0].toString();
            }
        });
        // do not spy on any methods
        testObject = cachingSpy(testObject);
        assertEquals("filename-1", testObject.fileNameAsParameter("filename-1"));
        assertNotNull(getClass().getClassLoader().getResource(
                "de.apaxo.test.TestObject-fileNameAsParameter-filename-1.dat"));
        assertEquals("filename-2", testObject.fileNameAsParameter("filename-2"));
        assertNotNull(getClass().getClassLoader().getResource(
                "de.apaxo.test.TestObject-fileNameAsParameter-filename-2.dat"));
        
    }
}
