package io.topiacoin.dht.util;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {

	@Test
	public void testObjectSerialization() {
		String expectedVar = "potato";
		String encoded = Utilities.objectToString(new ASerializableObject(expectedVar));
		Assert.assertNotNull(encoded);
		Assert.assertTrue(encoded.length() > 0);
		Object decoded = Utilities.objectFromString(encoded);
		Assert.assertNotNull(decoded);
		ASerializableObject decodedImpl = (ASerializableObject) decoded;
		Assert.assertEquals(expectedVar, decodedImpl.var);
		String sameEncoded = Utilities.objectToString(new ASerializableObject(expectedVar));
		String differentEncoded = Utilities.objectToString(new ASerializableObject(expectedVar + " but different"));
		Assert.assertEquals(encoded, sameEncoded);
		Assert.assertNotEquals(encoded, differentEncoded);
	}
}
