package io.topiacoin.core;

public interface EventAPI {

    /**
     * Posted when a workspace event has been received.
     * <p>
     * The classifier of this notification will be the workspaceGUID. The notification will not have any info.
     */
    String WORKSPACE_UPDATED_NOTIFICATION_TYPE = "workspaceUpdated";

    /**
     * Posted when a file event has been received.
     * <p>
     * The classifier of this notification will be the workspaceGUID. The notification will have the file GUID under the
     * 'fileID' key.
     */
    String FILE_UPDATED_NOTIFICATION_TYPE = "fileUpdated";

    /**
     * Starts the Event Fetcher. This will start the process of looking for events.
     * <p>
     * When new workspace events are detected, the Event Fetcher will post a notification to the Notification Center.
     * The event type will be based on the kinds of event that is being sent (workspace, file, etc). The classifier of
     * the notification will be the ID of the item the notification is related to (e.g. workspace ID, or file ID), or
     * null if the notification isn't related to a specific entity.
     */
    void startEventFetching();

    /**
     * Stops looking for events.
     */
    void stopEventFetching();

    /**
     * Returns true if the event fetcher is running. Otherwise, it returns false.
     *
     * @return
     */
    boolean isRunning();

}
