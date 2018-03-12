package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import static org.junit.Assert.*;

public class DefaultConfigurationTest {

	@org.junit.Test
	public void getAndSetConfigurationOption() {
		//Right now, there's a default property called 'foo'. Eventually, presumably there won't be. When that day comes, update these variables plox
		String oneOfTheDefaultPropertyNames = "foo";
		String theDefaultValueOfThatProperty = "bar";
		Configuration conf = new DefaultConfiguration();
		//Negative tests
		boolean shouldFail = true;
		try {
			conf.getConfigurationOption(null);
		} catch(IllegalArgumentException ex) {
			shouldFail = false;
		}
		assertTrue("Expected an IllegalArgumentException to be thrown, but it wasn't", !shouldFail);
		shouldFail = true;
		try {
			conf.getConfigurationOption("");
		} catch(IllegalArgumentException ex) {
			shouldFail = false;
		}
		assertTrue("Expected an IllegalArgumentException to be thrown, but it wasn't", !shouldFail);
		String val = conf.getConfigurationOption(oneOfTheDefaultPropertyNames);
		assertEquals("Property Values do not match...did the default config change?", theDefaultValueOfThatProperty, val);
		val = conf.getConfigurationOption("i am a potato");
		assertEquals("Expected non-existant confg option to be null, wasn't", null, val);
		shouldFail = true;
		try {
			conf.setConfigurationOption("testProp", "test");
		} catch(NotImplementedException ex) {
			shouldFail = false;
		}
		assertTrue("Expected a NotImplementedException to be thrown because Notifications haven't been implemented yet...did you implement them?", !shouldFail);
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		//This one shouldn't throw a NotImplementedException, because the value didn't change
		conf.setConfigurationOption("testProp", "test");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		shouldFail = true;
		try {
			conf.setConfigurationOption("testProp", null);
		} catch(NotImplementedException ex2) {
			shouldFail = false;
		}
		assertTrue("Expected a NotImplementedException to be thrown because Notifications haven't been implemented yet...did you implement them?", !shouldFail);
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", null, val);
	}
}