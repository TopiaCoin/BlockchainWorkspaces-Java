package io.topiacoin.core;

/**
 * The Configuration singleton holds the user editable runtime configuration for the Secrata Library. It defines methods
 * that allow the configuration to be read and modified. When the configuration is modified, a notification will be sent
 * out allowing all interested components to re-read their configuration from the server. The notification will include
 * the name of the configuration that has been changed along with its new value.
 */
public class Configuration {

    /**
     * Posted when a configuration value is changed. The classifier is the name of the notification that was changed.
     * The notification info contains the old value under the key 'oldValue' and the new value under the key
     * 'newValue'.
     */
    public static final String CONFIGURATION_DID_CHANGE_NOTIFICATION_TYPE = "ConfigurationDidChange";

    /**
     *
     */
    void setConfigurationOption(String name, Object value) {

    }

    /**
     *
     */
    Object getConfigurationOption(String name) {
        return null;
    }

    /**
     * It is expected that the Configuration singleton will have convienence methods added to it that simplify and
     * strongly type commonly used configuration values. Examples of such convienence methods might include:
     */
    Object getConfigurationOption(String name, Object defaultValue) {
        return null;
    }

    /**
     *
     */
    String getServerURL() {
        return null;
    }

    /**
     *
     */
    int getMaxSimultaneousUploads() {
        return 0;
    }

}
