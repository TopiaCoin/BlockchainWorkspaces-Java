package io.topiacoin.core.util;

import java.util.HashMap;
import java.util.Map;

public class Notification {

    private String notificationType ;
    private Object classifier ;
    private Map<String, Object> notificationInfo;

    public Notification(String notificationType, Object classifier, Map<String, Object> notificationInfo) {
        this.notificationType = notificationType;
        this.classifier = classifier;
        this.notificationInfo = new HashMap<String, Object>(notificationInfo);
    }

    public String getNotificationType() {
        return notificationType;
    }

    public Object getClassifier() {
        return classifier;
    }

    public Map<String, Object> getNotificationInfo() {
        return notificationInfo;
    }
}
