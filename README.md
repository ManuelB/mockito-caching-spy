mockito-caching-spy
===================

This project implements the possibility for a certain object to cache the return values of a function and then return when they are called again. This can be used to create unit tests from integration tests.

![Caching spy functionality](https://raw.github.com/ManuelB/mockito-caching-spy/master/src/site/resources/Caching-Spy.signavio.xml.png "Caching Spy functionality")

The image above shows how you can use the mocking spy. Basically you spy on a real object. If the return of a certain function is already available it will read from the file system. If not the original system will be called and the answer will be saved in target/test-classes/\*.dat. Now if you want to mock the big comples system just copy this file to src/test/resources.

To see some code examples:

  * [CachingSpyTest.java](https://github.com/ManuelB/mockito-caching-spy/blob/master/src/test/java/de/apaxo/test/CachingSpyTest.java)
  * [TestObject.java](https://github.com/ManuelB/mockito-caching-spy/blob/master/src/test/java/de/apaxo/test/TestObject.java)

To get this system do the following:

```shell
git clone https://github.com/ManuelB/mockito-caching-spy.git
mvn install
```

You will need git, mvn, and java.
 
This code is used in the [Apaxo GmbH platform](http://www.apaxo.de)
