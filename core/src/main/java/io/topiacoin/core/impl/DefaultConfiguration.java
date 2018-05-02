package io.topiacoin.core.impl;

import io.topiacoin.core.Configuration;
import io.topiacoin.util.NotificationCenter;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
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
        if (oldValue == null || (oldValue != null && newValue == null) || !oldValue.equals(newValue)) {
            Map<String, Object> notificationInfo = new HashMap<String, Object>();
            notificationInfo.put("key", key);
            notificationInfo.put("oldValue", oldValue);
            notificationInfo.put("value", newValue);
            _notificationCenter.postNotification(CONFIGURATION_DID_CHANGE_NOTIFICATION_TYPE, key, notificationInfo);
        }
    }

    /**
     * Sets a property. If the value changes as a result of this call, a Notification will be emitted
     *
     * @param name  the name of the configuration property to change
     * @param value the new value of the configuration property
     */
    public <T> void setConfigurationOption(String name, T value) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot set property for null or blank");
        }
        String oldValue = getConfigurationOption(name);
        String valueString = convertToString(value);
        if (valueString != null) {
            _overrides.setProperty(name, valueString);
        } else {
            _overrides.remove(name);
        }
        notifyOfConfigurationChange(name, oldValue, valueString);
    }

    /**
     * Returns a configuration property, or null if that property does not exist in the system.
     *
     * @param name the name of the configuration value to return
     *
     * @return a configuration property, or null if that property does not exist in the system
     *
     * @throws IllegalArgumentException if name is null or blank
     */
    public String getConfigurationOption(String name) throws IllegalArgumentException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot fetch property for null or blank");
        }
        return _overrides.getProperty(name);
    }

    /**
     * Returns the configuration value associated with the given name, converting it to the specified returnType if
     * possible.  If the property does not exist in the system, null is returned.
     *
     * @param name       the name of the configuration value to return
     * @param returnType The type to which the value is to be converted.
     *
     * @return a configuration property converted to the specified type, or null if that property does not exist in the
     * system
     *
     * @throws IllegalArgumentException if name is null or blank
     */
    public <T> T getConfigurationOption(String name, Class<T> returnType) throws IllegalArgumentException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot fetch property for null or blank");
        }
        String value = _overrides.getProperty(name);
        return (T) convertFromString(returnType, value);
    }

    /**
     * Returns the configuration value associated with the given name.  If no configuration value exists, the default
     * value will be returned.  The return value will be cast the default value's type.
     *
     * @param name         the name of the configuration value to return
     * @param defaultValue The value to return if the named option is not found in the configuration.
     *
     * @return the value of the configuration property, or null if no such property exists
     */
    @Override
    public <T> T getConfigurationOption(String name, T defaultValue) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Cannot fetch property for null or blank");
        }

        T returnValue = null;

        String value = _overrides.getProperty(name);
        if (value != null) {
            returnValue = (T) convertFromString(defaultValue.getClass(), value);
        } else {
            returnValue = defaultValue;
        }

        return returnValue;
    }

    /**
     * Converts a String to the requested type.
     *
     * @param targetType The type of object to which you want the string converted.
     * @param text       The string to be converted.
     *
     * @return An object of the requested type with the specified value.
     */
    private Object convertFromString(Class<?> targetType, String text) {
        if (text == null) {
            return null;
        }
        if (targetType.equals(String.class)) {
            return text;
        }
        PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
        editor.setAsText(text);
        return editor.getValue();
    }

    /**
     * Converts the specified value to a string.
     *
     * @param value The value to be converted.
     *
     * @return A String containing the given value.
     */
    private String convertToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value.getClass().equals(String.class)) {
            return (String) value;
        }
        PropertyEditor editor = PropertyEditorManager.findEditor(value.getClass());
        editor.setValue(value);
        return editor.getAsText();
    }

}
