package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.util.Notification;
import io.topiacoin.util.NotificationCenter;
import io.topiacoin.util.NotificationHandler;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
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
		//Test that overriding default values works
		conf.setConfigurationOption(oneOfTheDefaultPropertyNames, theDefaultValueOfThatProperty + "1");
		val = conf.getConfigurationOption(oneOfTheDefaultPropertyNames);
		assertEquals("Property Values do not match...set didn't work?", theDefaultValueOfThatProperty + "1", val);
		//Test that nullifying an override reverts back to the default config value...?
		conf.setConfigurationOption(oneOfTheDefaultPropertyNames, null);
		val = conf.getConfigurationOption(oneOfTheDefaultPropertyNames);
		assertEquals("Property Values do not match...set didn't work?", theDefaultValueOfThatProperty, val);
	}

	@org.junit.Test
	public void setConfigurationOptionSendsNotificationsAppropriately() {
		Configuration conf = new DefaultConfiguration();
		NotificationCenter center = NotificationCenter.defaultCenter();
		//Make sure it works for unclassified handlers
		final List<Map<String, Object>> gotNotifications = new ArrayList<Map<String, Object>>();
		center.addHandler(new NotificationHandler() {
			public void handleNotification(Notification notification) {
				gotNotifications.add(notification.getNotificationInfo());
			}
		}, "ConfigurationDidChange", null);
		//Also make sure it works for classified handlers (testProp2)
		final List<Map<String, Object>> gotNotificationsWithClassifier = new ArrayList<Map<String, Object>>();
		center.addHandler(new NotificationHandler() {
			public void handleNotification(Notification notification) {
				gotNotificationsWithClassifier.add(notification.getNotificationInfo());
			}
		}, "ConfigurationDidChange", "testProp2");
		conf.setConfigurationOption("testProp", "test");
		String val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		//Make sure 1 Notification got sent.
		assertEquals("Didn't get a Notification on Config update", 1, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp", gotNotifications.get(0).get("key"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(0).get("value"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(0).get("oldValue"));
		assertEquals("Classifier Notification Handler count incorrect", 0, gotNotificationsWithClassifier.size());

		//Since the value isn't changing, a second Notification should not be sent
		conf.setConfigurationOption("testProp", "test");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		assertEquals("Got a Notification when I shouldn't have", 1, gotNotifications.size());
		assertEquals("Classifier Notification Handler count incorrect", 0, gotNotificationsWithClassifier.size());

		//Since the value is getting changed this time, a second notification should be sent
		conf.setConfigurationOption("testProp", "test2");
		val = conf.getConfigurationOption("testProp");
		assertEquals("Property Values do not match...set didn't work?", "test2", val);
		assertEquals("Didn't get a Notification on Config update", 2, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp", gotNotifications.get(1).get("key"));
		assertEquals("NotificationInfo wrong", "test2", gotNotifications.get(1).get("value"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(1).get("oldValue"));
		assertEquals("Classifier Notification Handler count incorrect", 0, gotNotificationsWithClassifier.size());

		//Now test the classified value, should send a notification to both handlers
		conf.setConfigurationOption("testProp2", "test");
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", "test", val);
		assertEquals("Didn't get a Notification on Config update", 3, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotifications.get(2).get("key"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(2).get("value"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(2).get("oldValue"));
		assertEquals("Classifier Notification Handler count incorrect", 1, gotNotificationsWithClassifier.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotificationsWithClassifier.get(0).get("key"));
		assertEquals("NotificationInfo wrong", "test", gotNotificationsWithClassifier.get(0).get("value"));
		assertEquals("NotificationInfo wrong", null, gotNotificationsWithClassifier.get(0).get("oldValue"));

		//Should send a Notification if we set the value to null
		conf.setConfigurationOption("testProp2", null);
		val = conf.getConfigurationOption("testProp2");
		assertEquals("Property Values do not match...set didn't work?", null, val);
		assertEquals("Didn't get a Notification on Config update", 4, gotNotifications.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotifications.get(3).get("key"));
		assertEquals("NotificationInfo wrong", null, gotNotifications.get(3).get("value"));
		assertEquals("NotificationInfo wrong", "test", gotNotifications.get(3).get("oldValue"));
		assertEquals("Classifier Notification Handler count incorrect", 2, gotNotificationsWithClassifier.size());
		assertEquals("NotificationInfo wrong", "testProp2", gotNotificationsWithClassifier.get(1).get("key"));
		assertEquals("NotificationInfo wrong", null, gotNotificationsWithClassifier.get(1).get("value"));
		assertEquals("NotificationInfo wrong", "test", gotNotificationsWithClassifier.get(1).get("oldValue"));
	}

	@Test
	public void testGetAndSetWithDefaultValues() throws Exception {
		Configuration conf = new DefaultConfiguration();

		Integer intValue;
		Long longValue;
		String stringValue;

		intValue = conf.getConfigurationOption("prop1", 1234);
		assertEquals ( 1234, (int)intValue) ;

		conf.setConfigurationOption("prop1", 23456);

		intValue = conf.getConfigurationOption("prop1", 1234) ;
		assertEquals ( 23456, (int)intValue) ;

		stringValue = conf.getConfigurationOption("prop1");
		assertEquals("23456", stringValue);

		conf.setConfigurationOption("prop2", "99876");

		stringValue = conf.getConfigurationOption("prop2");
		assertEquals ( "99876", stringValue) ;

		intValue = conf.getConfigurationOption("prop2", int.class) ;
		assertEquals ( 99876, (int)intValue);

		longValue = conf.getConfigurationOption("prop2", Long.class) ;
		assertEquals ( 99876L, (long)longValue);

		stringValue = conf.getConfigurationOption("prop2", String.class) ;
		assertEquals ( "99876", stringValue) ;
	}
}