package io.topiacoin.model;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileTagTest {

    @Test
    public void testDefaultConstructor() throws Exception {
        FileTag fileTag = new FileTag();

        assertNull ( fileTag.getScope()) ;
        assertNull (fileTag.getValue()) ;
    }

    @Test
    public void testConstructor() throws Exception {

        String scope = "public" ;
        String value = "flabbit" ;

        FileTag fileTag = new FileTag(scope, value) ;

        assertEquals(scope, fileTag.getScope());
        assertEquals(value, fileTag.getValue());
    }

    @Test
    public void testBasicAccessors() throws Exception {

        String scope = "public" ;
        String value = "flabbit" ;

        FileTag fileTag = new FileTag() ;

        // Check the Scope Accessors
        assertNull ( fileTag.getScope()) ;
        fileTag.setScope(scope);
        assertEquals(scope, fileTag.getScope());
        fileTag.setScope(null);
        assertNull ( fileTag.getScope()) ;

        // Check the Value Accessors
        assertNull (fileTag.getValue()) ;
        fileTag.setValue(value);
        assertEquals(value, fileTag.getValue());
        fileTag.setValue(null);
        assertNull (fileTag.getValue()) ;

    }

    @Test
    public void testEqualsAndHashCode() throws Exception {

        String scope = "public" ;
        String value = "flabbit" ;

        FileTag fileTag1 = new FileTag(scope, value) ;
        FileTag fileTag2 = new FileTag(scope, value) ;

        assertEquals ( fileTag1, fileTag1) ;
        assertEquals(fileTag2, fileTag2);
        assertEquals(fileTag1, fileTag2);
        assertEquals(fileTag2, fileTag1);

        assertEquals(fileTag1.hashCode(), fileTag2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeOfBareObjects() throws Exception {

        FileTag fileTag1 = new FileTag() ;
        FileTag fileTag2 = new FileTag() ;

        assertEquals ( fileTag1, fileTag1) ;
        assertEquals(fileTag2, fileTag2);
        assertEquals(fileTag1, fileTag2);
        assertEquals(fileTag2, fileTag1);

        assertEquals(fileTag1.hashCode(), fileTag2.hashCode());

    }

}
