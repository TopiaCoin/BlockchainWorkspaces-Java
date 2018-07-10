package io.topiacoin.model.provider;

import io.topiacoin.model.DataModel;
import io.topiacoin.model.Message;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.MessageAlreadyExistsException;
import io.topiacoin.model.exceptions.NoSuchMessageException;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class DataModelMessageTest {

    public abstract DataModel initDataModel();

    // -------- Message Tests --------

    @Test
    public void testMessageCRUD() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        List<Message> messages;

        messages = dataModel.getMessagesInWorkspace(workspaceID);
        assertNotNull(messages);
        assertEquals(0, messages.size());

        dataModel.addMessageToWorkspace(workspaceID, message);

        messages = dataModel.getMessagesInWorkspace(workspaceID);
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
        assertNotSame(message, messages.get(0));

        Message fetchedMessage = dataModel.getMessage(messageID);
        assertNotNull(fetchedMessage);
        assertEquals(message, fetchedMessage);
        assertNotSame(message, fetchedMessage);

        message.setText("bar");
        dataModel.updateMessageInWorkspace(workspaceID, message);

        messages = dataModel.getMessagesInWorkspace(workspaceID);
        assertNotNull(messages);
        assertEquals(1, messages.size());
        assertEquals(message, messages.get(0));
        assertNotSame(message, messages.get(0));

        dataModel.removeMessageFromWorkspace(workspaceID, message);

        messages = dataModel.getMessagesInWorkspace(workspaceID);
        assertNotNull(messages);
        assertEquals(0, messages.size());
    }

    @Test
    public void testChangingAddedMessageDoesNotChangeModel() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.addMessageToWorkspace(workspaceID, message);

        message.setText("bar");

        Message fetchedMessage = dataModel.getMessage(messageID);
        assertNotEquals(message, fetchedMessage);
    }

    @Test
    public void testChangingFetchedMessageDoesNotChangeModel() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        dataModel.addMessageToWorkspace(workspaceID, message);

        Message fetchedMessage = dataModel.getMessage(messageID);
        fetchedMessage.setText("bar");

        Message fetchedMessage2 = dataModel.getMessage(messageID);
        assertEquals(message, fetchedMessage2);
        assertNotEquals(fetchedMessage, fetchedMessage2);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testGetMessagesFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();

        DataModel dataModel = initDataModel();
        List<Message> messages;

            // Expect a NoSuchWorkspaceException
            messages = dataModel.getMessagesInWorkspace(workspaceID);
    }

    @Test(expected = NoSuchMessageException.class)
    public void testGetNonExistentMessage() throws Exception {
        long messageID = new Random().nextLong();

        DataModel dataModel = initDataModel();
        Message message;

        // Expect a NoSuchMessageException
        message = dataModel.getMessage(messageID);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testAddMessageToNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        // Expect a NoSuchWorkspaceException
        dataModel.addMessageToWorkspace(workspaceID, message);
    }

    @Test(expected = MessageAlreadyExistsException.class)
    public void testAddDuplicateMessageToWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);
        dataModel.addMessageToWorkspace(workspaceID, message);

        // Expect a NoSuchWorkspaceException
        dataModel.addMessageToWorkspace(workspaceID, message);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testUpdateMessageInNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        // Expect a NoSuchWorkspaceException
        dataModel.updateMessageInWorkspace(workspaceID, message);
    }

    @Test(expected = NoSuchMessageException.class)
    public void testUpdateNonExistentMessageInWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        // Expect a NoSuchMessageException
        dataModel.updateMessageInWorkspace(workspaceID, message);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testRemoveMessageFromNonExistentWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        // Expect a NoSuchWorkspaceException
        dataModel.removeMessageFromWorkspace(workspaceID, message);
    }

    @Test(expected = NoSuchMessageException.class)
    public void testRemoveNonExistentMessageFromWorkspace() throws Exception {
        long workspaceID = new Random().nextLong();
        long messageID = new Random().nextLong();
        String authorID = UUID.randomUUID().toString();

        Workspace workspace = new Workspace();
        workspace.setGuid(workspaceID);
        workspace.setName("Sample Workspace");

        Message message = new Message();
        message.setGuid(messageID);
        message.setText("foo");
        message.setAuthorID(authorID);

        DataModel dataModel = initDataModel();

        dataModel.addWorkspace(workspace);

        // Expect a NoSuchMessageException
        dataModel.removeMessageFromWorkspace(workspaceID, message);
    }


}
