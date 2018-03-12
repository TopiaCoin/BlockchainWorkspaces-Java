package io.topiacoin.core;

/**
 * The Configuration singleton holds the user editable runtime configuration for the Secrata Library. It defines methods
 * that allow the configuration to be read and modified. When the configuration is modified, a notification will be sent
 * out allowing all interested components to re-read their configuration from the server. The notification will include
 * the name of the configuration that has been changed along with its new value.
 */
public interface Configuration {

    /**
     * Updates the configuration with the specified name and value. If (and only if) the value is different than the one already stored, a Notification will be
     * posted, indicating that the configuration has changed
     * @param name the name of the configuration property to change
     * @param value the new value of the configuration property
     */
    void setConfigurationOption(String name, String value);

    /**
     * Returns the configuration value associated with the given name, or null if no such name exists
     * @param name the name of the configuration value to return
     * @return the value of the configuration property, or null if no such property exists
     */
    String getConfigurationOption(String name);
}
