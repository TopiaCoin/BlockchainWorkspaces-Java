/*
 * Created on Oct 23, 2005
 */

package io.topiacoin.model.exceptions;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static org.junit.Assert.*;

/**
 * Abstract Test for testing Exception Classes. This test case will verify
 * that the Throwable class under test implements all 4 constructors defined
 * by the Java 1.4 Throwable class. In addition, it contains a coherency test
 * to verify that the Throwable class being tested matches the test case being
 * run. Furthermore, it tests to verify that the Exception being tested
 * properly inherits from the java.lang.Throwable base class.
 *
 * @note This test assumes that the test case for a Throwable class is name
 *       {Throwable}Test. For example, the test for the class UnknownException
 *       should be named UnknownExceptionTest. If it isn't, then the Coherency
 *       test will fail.
 *
 * @author john
 * @since Oct 23, 2005
 */
public abstract class AbstractThrowableTest
{
    /**
     * Abstract method that returns the Class object representing the
     * Throwable Class that is under test.
     *
     * @return The Class object representing the Throwable Class under test.
     */
    public abstract Class getExceptionClass ( ) ;

    /**
     * Self diagnostics check to ensure that the Throwable Class being tested
     * matches the name of the Test Case being executed. This test is included
     * to catch any copy and paste mistakes that may occur when duplicating
     * subclasses of this test.
     */
    @Test
    public void testThrowableTestConherency ( )
    {
        String throwableTestType = this.getClass ( ).getName ( ) ;
        String throwableType = throwableTestType.substring ( 0,
                throwableTestType.lastIndexOf ( "Test" ) ) ;

        assertEquals ( "The test name and exception type do not match",
                throwableType,
                getExceptionClass ( ).getName ( ) ) ;
    }

    /**
     * Tests that the Throwable Class being tested actually inherits from
     * java.lang.Throwable.
     */
    @Test
    public void testThrowableInheritance ( )
    {
        assertTrue ( Throwable.class.isAssignableFrom ( getExceptionClass ( ) ) ) ;
    }

    /**
     * Tests that the Throwable Class being tested declares a default
     * constructor as defined by the Java 1.4 Throwable class.
     */
    @Test
    public void testDefaultConstructor ( ) throws Exception
    {
        Class excepClass = getExceptionClass ( ) ;
        Throwable t = null ;

        try
        {
            t = (Throwable) excepClass.newInstance ( ) ;
        }
        catch ( InstantiationException e )
        {
            fail ( "The Class is missing the default Constructor" ) ;
        }

        assertNull ( "Message was set unexpectedly", t.getMessage ( ) ) ;
        assertNull ( "Cause was set unexpectedly", t.getCause ( ) ) ;
    }

    /**
     * Tests that the Throwable Class being tested declares a constructor with
     * a single String argument as defined by the Java 1.4 Throwable class.
     */
    @Test
    public void testStringConstructor ( ) throws Exception
    {
        Class excepClass = getExceptionClass ( ) ;
        Throwable t = null ;

        Class[] types = new Class[1] ;
        types[0] = String.class ;

        Object[] params = new Object[1] ;
        params[0] = "Test" ;

        try
        {
            Constructor constr = excepClass.getConstructor ( types ) ;

            t = (Throwable) constr.newInstance ( params ) ;
        }
        catch ( NoSuchMethodException e )
        {
            fail ( "The Class is missing the (String) Constructor" ) ;
        }

        assertEquals ( "Message was not set", params[0], t.getMessage ( ) ) ;
        assertNull ( "Cause was set unexpectedly", t.getCause ( ) ) ;
    }

    /**
     * Tests that the Throwable Class being tested declares a constructor with
     * a single Throwable argument as defined by the Java 1.4 Throwable class.
     */
    @Test
    public void testThrowableConstructor ( ) throws Exception
    {
        Class excepClass = getExceptionClass ( ) ;
        Throwable t = null ;

        Class[] types = new Class[1] ;
        types[0] = Throwable.class ;

        Object[] params = new Object[1] ;
        params[0] = new Exception ( "Test" ) ;

        try
        {
            Constructor constr = excepClass.getConstructor ( types ) ;

            t = (Throwable) constr.newInstance ( params ) ;
        }
        catch ( NoSuchMethodException e )
        {
            fail ( "The Class is missing the (Throwable) Constructor" ) ;
        }

        assertEquals ( "Cause was not set", params[0], t.getCause ( ) ) ;
        assertEquals ( "Message not set as Expected",
                ( (Throwable) params[0] ).getMessage ( ),
                t.getCause ( ).getMessage ( ) ) ;
    }

    /**
     * Tests that the Throwable Class being tested declares a constructor with
     * both a String argument and a Throwable argument as defined by the Java
     * 1.4 Throwable class.
     */
    @Test
    public void testStringThrowableConstructor ( ) throws Exception
    {
        Class excepClass = getExceptionClass ( ) ;
        Throwable t = null ;

        Class[] types = new Class[2] ;
        types[0] = String.class ;
        types[1] = Throwable.class ;

        Object[] params = new Object[2] ;
        params[0] = "Test" ;
        params[1] = new Exception ( "Test" ) ;

        try
        {
            Constructor constr = excepClass.getConstructor ( types ) ;

            t = (Throwable) constr.newInstance ( params ) ;
        }
        catch ( NoSuchMethodException e )
        {
            fail ( "The Class is missing the (String, Throwable) Constructor" ) ;
        }

        assertEquals ( "Message was not set", params[0], t.getMessage ( ) ) ;
        assertEquals ( "Cause was not set", params[1], t.getCause ( ) ) ;
    }

}
