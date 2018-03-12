package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.util.NotificationCenter;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DefaultConfiguration implements Configuration {

	private final Properties DEFAULT_PROPERTIES = new Properties();
	protected Properties _overrides = new Properties(DEFAULT_PROPERTIES);
	private final NotificationCenter _notificationCenter = NotificationCenter.defaultCenter();
	/**
	 * Posted when a configuration value is changed. The classifier is the name of the notification that was changed.
	 * The notification info contains the old value under the key 'oldValue' and the new value under the key
	 * 'newValue'.
	 */
	private static final String CONFIGURATION_DID_CHANGE_NOTIFICATION_TYPE = "ConfigurationDidChange";

	/**
	 * Initializes DefaultConfiguration, setting sane default properties in the process
	 */
	public DefaultConfiguration() {
		//Initialize defaults here.
		DEFAULT_PROPERTIES.setProperty("foo", "bar"); //If you remove this property, you need to update the Unit Test DefaultConfigurationTest
	}

	private void notifyOfConfigurationChange(String key, String oldValue, String newValue) {
		if(oldValue == null || (oldValue != null && newValue == null) || !oldValue.equals(newValue)) {
			Map<String, Object> notificationInfo = new HashMap<String, Object>();
			notificationInfo.put("key", key);
			notificationInfo.put("oldValue", oldValue);
			notificationInfo.put("value", newValue);
			_notificationCenter.postNotification(CONFIGURATION_DID_CHANGE_NOTIFICATION_TYPE, key, notificationInfo);
		}
	}

	/**
	 * Sets a property. If the value changes as a result of this call, a Notification will be emitted
	 * @param name the name of the configuration property to change
	 * @param value the new value of the configuration property
	 */
	public void setConfigurationOption(String name, String value) {
		String oldValue = getConfigurationOption(name);
		if(value != null) {
			_overrides.setProperty(name, value);
		} else {
			_overrides.remove(name);
		}
		notifyOfConfigurationChange(name, oldValue, value);
	}

	/**
	 * Returns a configuration property, or null if that property does not exist in the system.
	 * @param name the name of the configuration value to return
	 * @return a configuration property, or null if that property does not exist in the system
	 * @throws IllegalArgumentException if name is null or blank
	 */
	public String getConfigurationOption(String name) throws IllegalArgumentException {
		if(name == null || name.length() == 0) {
			throw new IllegalArgumentException("Cannot fetch property for null or blank");
		}
		return _overrides.getProperty(name);
	}
}
