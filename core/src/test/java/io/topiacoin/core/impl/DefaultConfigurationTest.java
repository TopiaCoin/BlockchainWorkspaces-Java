package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
		final List<Map<String, Object>> gotNotifications = new ArrayList<Map<String, Object>>();
		center.addHandler(new NotificationHandler() {
			public void handleNotification(Notification notification) {
				gotNotifications.add(notification.getNotificationInfo());
			}
		}, "ConfigurationDidChange", null);
		conf.setConfigurationOption("testProp", "test");
		String val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		//Make sure 1 Notification got sent.
		assertEquals("Didn't get a Notification on Config update", 1, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp", gotNotifications.get(0).get("key"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(0).get("value"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(0).get("oldValue"));

		//Since the value isn't changing, a second Notification should not be sent
		conf.setConfigurationOption("testProp", "test");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		assertEquals("Got a Notification when I shouldn't have", 1, gotNotifications.size());

		//Since the value is getting changed this time, a second notification should be sent
		conf.setConfigurationOption("testProp", "test2");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test2", val);
		assertEquals("Didn't get a Notification on Config update", 2, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp", gotNotifications.get(1).get("key"));
		assertEquals("NotificationInfo wrong", "test2", gotNotifications.get(1).get("value"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(1).get("oldValue"));

		//Quick sanity check, should send a Notification
		conf.setConfigurationOption("testProp2", "test");
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		assertEquals("Didn't get a Notification on Config update", 3, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotifications.get(2).get("key"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(2).get("value"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(2).get("oldValue"));
		//Should send a Notification if we set the value to null
		conf.setConfigurationOption("testProp2", null);
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", null, val);
		assertEquals("Didn't get a Notification on Config update", 4, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotifications.get(3).get("key"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(3).get("value"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(3).get("oldValue"));
	}
}