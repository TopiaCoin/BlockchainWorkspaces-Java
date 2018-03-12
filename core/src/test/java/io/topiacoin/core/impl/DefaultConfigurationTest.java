package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DefaultConfigurationTest {

	@org.junit.Test
	public void getConfigurationOptionNegativeTests() {
		Configuration conf = new DefaultConfiguration();
		//Negative tests
		try {
			conf.getConfigurationOption(null);
			fail("Expected an IllegalArgumentException to be thrown, but it wasn't");
		} catch (IllegalArgumentException ex) {
			//NOP, expected exception
		}
		try {
			conf.getConfigurationOption("");
			fail("Expected an IllegalArgumentException to be thrown, but it wasn't");
		} catch (IllegalArgumentException ex) {
			//NOP, expected exception
		}
	}

	@org.junit.Test
	public void getAndSetConfigurationOption() {
		//Right now, there's a default property called 'foo'. Eventually, presumably there won't be. When that day comes, update these variables plox
		String oneOfTheDefaultPropertyNames = "foo";
		String theDefaultValueOfThatProperty = "bar";
		Configuration conf = new DefaultConfiguration();
		String val = conf.getConfigurationOption(oneOfTheDefaultPropertyNames);
		assertEquals("Property Values do not match...did the default config change?", theDefaultValueOfThatProperty, val);
		val = conf.getConfigurationOption("i am a potato");
		assertEquals("Expected non-existant confg option to be null, wasn't", null, val);
		conf.setConfigurationOption("testProp", "test");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		conf.setConfigurationOption("testProp", "test");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		conf.setConfigurationOption("testProp", null);
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", null, val);
	}

	@org.junit.Test
	public void setConfigurationOptionSendsNotificationsAppropriately() {
		Configuration conf = new DefaultConfiguration();
		NotificationCenter center = NotificationCenter.defaultCenter();
		final List<Boolean> gotNotifications = new ArrayList<Boolean>();
		center.addHandler(new NotificationHandler() {
			public void handleNotification(Notification notification) {
				gotNotifications.add(true);
			}
		}, "ConfigurationDidChange", null);
		conf.setConfigurationOption("testProp", "test");
		String val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		//Make sure 1 Notification got sent.
		assertEquals("Didn't get a Notification on Config update", 1, gotNotifications.size());
		//Since the value isn't changing, a second Notification should not be sent
		conf.setConfigurationOption("testProp", "test");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		//Since the value is getting changed this time, a second notification should be sent
		conf.setConfigurationOption("testProp", "test2");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test2", val);
		assertEquals("Didn't get a Notification on Config update", 2, gotNotifications.size());
		//Quick sanity check, should send a Notification
		conf.setConfigurationOption("testProp2", "test");
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		assertEquals("Didn't get a Notification on Config update", 3, gotNotifications.size());
		//Should send a Notification if we set the value to null
		conf.setConfigurationOption("testProp2", null);
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", null, val);
		assertEquals("Didn't get a Notification on Config update", 4, gotNotifications.size());
	}
}