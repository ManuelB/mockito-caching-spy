package de.apaxo.test;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mockito.MockingDetails;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/**
 * This class can spy on an arbitrary Object and will intercept all given
 * methods. It will search in the test directory for the given method return and
 * return it. If it is not found the original call will be executed and cached
 * in the test directory. With this method a unit test can be generated out of
 * an integration test.
 * 
 * The files will be placed directly at the root level of the executed folder.
 * If you want to keep them you have to copy them into the src folder e.g. in a
 * meven project:
 * 
 * cp target/test-classes/*.dat src/test/resources
 * 
 * @author Manue Blechschmidt <blechschmidt@apaxo.de>
 * 
 */
public class CachingSpy implements Answer<Object> {

    /**
     * Logger for our CachingSpy
     */
    private static final Logger                      log                         = Logger.getLogger(CachingSpy.class
                                                                                         .getName());

    /**
     * This answer will be used by all caching spy instances.
     */
    private static CachingSpy                        CACHING_SPY_ANSWERS         = new CachingSpy();

    /**
     * Holds a map which functions of which object should be cached.
     */
    private static Map<Object, Map<Method, Boolean>> shouldClassMethodBeCached   = new HashMap<Object, Map<Method, Boolean>>();

    /**
     * This can be used to generate custom file names in case the hashcode is
     * not implemented in a good way.
     */
    private static Map<Method, FileNameConstructor>  methods2fileNameConstructor = new HashMap<Method, FileNameConstructor>();

    /**
     * This class can not be instantiated by anyone.
     */
    private CachingSpy() {

    }

    /**
     * This method intercepts all calls to object and returns if available a
     * serialized object from the cache in the test directory. If not available
     * the normal flow is executed and the result is cached for further calls.
     * 
     * All method calls including methods of super classes will be intercepted.
     * 
     * All parameters and return values must be serializable.
     * 
     * @param object
     *            the object to spy on
     * @param methods
     *            list of methods which should be cached
     * @return the object wrapped in a spy
     */
    public static <T> T cachingSpy(T object) {
        return cachingSpy(object, null);
    }

    /**
     * This method intercepts all calls to object and returns if available a
     * serialized object from the cache in the test directory. If not available
     * the normal flow is executed and the result is cached for further calls.
     * 
     * All parameters and return values must be serializable.
     * 
     * You can supply a list of methods that should be cached. If none is
     * supplied all methods of the class including super classes will be
     * intercepted.
     * 
     * @param object
     *            the object to spy on
     * @param methodsToCache
     *            list of methods which should be cached
     * @return the object wrapped in a spy
     */
    public static <T> T cachingSpy(T object, Method[] methodsToCache) {
        MockingDetails mockingDetails = Mockito.mockingDetails(object);
        if (mockingDetails.isMock() || mockingDetails.isSpy()) {
            throw new IllegalArgumentException("The object that you supplied "
                    + object + " is already a mock."
                    + " Please create a new one.");
        }
        @SuppressWarnings("unchecked")
        Class<T> clazz = (Class<T>) object.getClass();
        T spiedObject = mock(clazz, withSettings().spiedInstance(object)
                .defaultAnswer(CACHING_SPY_ANSWERS));
        addMethodsToSpiedAndCachedMethods(methodsToCache, clazz, spiedObject);

        return spiedObject;
    }

    /**
     * This function adds the methods to the caching spy.
     * 
     * @param methodsToCache
     * @param clazz
     * @param spiedObject
     */
    private static <T> void addMethodsToSpiedAndCachedMethods(
            Method[] methodsToCache, Class<T> clazz, T spiedObject) {
        // if no methods are given cache all methods
        if (methodsToCache == null) {
            methodsToCache = clazz.getMethods();
        }
        // go through all methods
        for (Method method : methodsToCache) {
            // only spy public methods
            if (Modifier.isPublic(method.getModifiers())) {
                Map<Method, Boolean> shouldMethodBeCached = shouldClassMethodBeCached
                        .get(spiedObject);
                if (shouldMethodBeCached == null) {
                    shouldMethodBeCached = new HashMap<Method, Boolean>();
                    shouldClassMethodBeCached.put(spiedObject,
                            shouldMethodBeCached);
                }
                log.fine("Intercepting " + method.getName() + " on "
                        + clazz.getName() + " instance: " + spiedObject);
                // save that the method should be cached
                shouldMethodBeCached.put(method, true);
            }
        }
    }

    /**
     * This function creates a decorator for an object that has a final class.
     * 
     * @param clazz
     * @param object
     * @return
     */
    public static <T extends K, K> K cachingSpyForFinalClass(Class<K> clazz,
            final T object) {
        return cachingSpyForFinalClass(clazz, object, null);
    }

    /**
     * This function creates a decorator for an object that has a final class.
     * 
     * @param clazz
     * @param object
     * @return
     */
    public static <T extends K, K> K cachingSpyForFinalClass(Class<K> clazz,
            final T object, Method[] methodsToCache) {
        MockingDetails mockingDetails = Mockito.mockingDetails(object);
        if (mockingDetails.isMock() || mockingDetails.isSpy()) {
            throw new IllegalArgumentException("The object that you supplied "
                    + object + " is already a mock."
                    + " Please create a new one.");
        }
        K spiedObject = mock(clazz,
                withSettings().defaultAnswer(new Answer<Object>() {

                    @Override
                    public Object answer(InvocationOnMock invocation)
                            throws Throwable {
                        // if the method should not be cached just return the
                        // real
                        // results
                        if (!shouldClassMethodBeCached.containsKey(invocation
                                .getMock())
                                || !shouldClassMethodBeCached.get(
                                        invocation.getMock()).containsKey(
                                        invocation.getMethod())) {
                            Method methodOnRealObject = object.getClass()
                                    .getMethod(
                                            invocation.getMethod().getName(),
                                            invocation.getMethod()
                                                    .getParameterTypes());

                            return methodOnRealObject.invoke(object,
                                    invocation.getArguments());
                        }
                        String fileName = generateFileNameForInvocation(invocation);
                        Object returnValue = tryToFindFileAndUnserialize(fileName);
                        if (returnValue == null) {
                            Method methodOnRealObject = object.getClass()
                                    .getMethod(
                                            invocation.getMethod().getName(),
                                            invocation.getMethod()
                                                    .getParameterTypes());
                            returnValue = methodOnRealObject.invoke(object,
                                    invocation.getArguments());
                            log.fine("Saving anwser to call to file: "
                                    + fileName);
                            serializeAndSaveToFile(fileName, returnValue);
                        } else {
                            log.fine("Reading anwser for call from file: "
                                    + fileName);
                        }
                        return returnValue;
                    }
                }));

        addMethodsToSpiedAndCachedMethods(methodsToCache, clazz, spiedObject);
        return spiedObject;
    }

    /**
     * Implement answer for caching spy.
     * 
     * @param invocation
     *            the invocation
     */
    @Override
    public Object answer(InvocationOnMock invocation) throws Throwable {
        // if the method should not be cached just return the real
        // results
        if (!shouldClassMethodBeCached.containsKey(invocation.getMock())
                || !shouldClassMethodBeCached.get(invocation.getMock())
                        .containsKey(invocation.getMethod())) {
            return invocation.callRealMethod();
        }
        String fileName = generateFileNameForInvocation(invocation);
        Object returnValue = tryToFindFileAndUnserialize(fileName);
        if (returnValue == null) {
            returnValue = invocation.callRealMethod();
            log.fine("Saving anwser to call to file: " + fileName);
            serializeAndSaveToFile(fileName, returnValue);
        } else {
            log.fine("Reading anwser for call from file: " + fileName);
        }
        return returnValue;
    }

    /**
     * Serialize the return value and save ot to the file.
     * 
     * @param fileName
     * @param returnValue
     */
    private static void serializeAndSaveToFile(String fileName,
            Object returnValue) {
        File directory;
        try {
            directory = new File(CachingSpy.class.getClassLoader()
                    .getResource("").toURI());
            File newFile = new File(directory, fileName);
            ObjectOutputStream out = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(newFile)));
            out.writeObject(returnValue);
            out.close();
        } catch (URISyntaxException e) {
            log.log(Level.SEVERE, "Exception was thrown", e);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Exception was thrown", e);
        }
    }

    /**
     * Try to find the file and unserialize the content as Java Objects. If
     * nothing is found just return null.
     * 
     * @param fileName
     * @return
     */
    private static Object tryToFindFileAndUnserialize(String fileName) {
        URL fileURL = CachingSpy.class.getClassLoader().getResource(fileName);
        if (fileURL == null) {
            log.fine("Did not find file name: " + fileName);
            return null;
        } else {
            File file;
            try {
                file = new File(fileURL.toURI());
            } catch (URISyntaxException e1) {
                log.log(Level.SEVERE, "Exception was thrown", e1);
                return null;
            }
            try {
                ObjectInputStream in = new ObjectInputStream(
                        new BufferedInputStream(new FileInputStream(file)));
                Object o = in.readObject();
                in.close();
                return o;
            } catch (FileNotFoundException e) {
                log.log(Level.FINE, "Exception was thrown", e);
                return null;
            } catch (IOException e) {
                log.log(Level.SEVERE, "Exception was thrown", e);
                return null;
            } catch (ClassNotFoundException e) {
                log.log(Level.SEVERE, "Exception was thrown", e);
                return null;
            }
        }
    }

    /**
     * Generates a fileName based on the class name, method signature and the
     * arguments.
     * 
     * @param invocation
     * @return
     */
    private static String generateFileNameForInvocation(
            InvocationOnMock invocation) {
        Method method = invocation.getMethod();
        Class<?> clazz = method.getDeclaringClass();
        String fileName = clazz.getName() + "-" + method.getName();

        if (methods2fileNameConstructor.containsKey(invocation.getMethod())) {
            fileName += "-"+methods2fileNameConstructor.get(invocation.getMethod())
                    .generateFileName(invocation.getArguments());
        } else {
            // it is forbidden to have cyclic references in an argument
            // http://docs.oracle.com/javase/7/docs/api/java/util/Arrays.html#deepHashCode(java.lang.Object%5B%5D)
            int hashCode = Arrays.deepHashCode(invocation.getArguments());
            fileName += "-" + hashCode;
        }

        fileName += ".dat";
        return fileName;
    }
    
    /**
     * This can be used to define an own file name constructor for naming files
     * in case hashCode is implemented in a bad way.
     * 
     * @param method
     * @param fileNameConstructor
     */
    public static void addFileNameConstructor(Method method, FileNameConstructor fileNameConstructor) {
        methods2fileNameConstructor.put(method, fileNameConstructor);
    }

    /**
     * This can be used to remove an own file name constructor for naming files
     * in case hashCode is implemented in a bad way.
     * 
     * @param method
     */
    public static void removeFileNameConstructor(Method method) {
        methods2fileNameConstructor.remove(method);
    }
    
    /**
     * This can be implemented by someone who wants to change how the files are
     * named for a certain method.
     * 
     * @author hu
     * 
     */
    public interface FileNameConstructor {
        public String generateFileName(Object[] arguments);
    }
}
