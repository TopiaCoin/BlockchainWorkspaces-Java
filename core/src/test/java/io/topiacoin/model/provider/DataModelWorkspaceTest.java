package io.topiacoin.model.provider;

import io.topiacoin.model.DataModel;
import io.topiacoin.model.Workspace;
import io.topiacoin.model.exceptions.NoSuchWorkspaceException;
import io.topiacoin.model.exceptions.WorkspaceAlreadyExistsException;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

public abstract class DataModelWorkspaceTest {

    public abstract DataModel initDataModel();

    // -------- Workspace Tests --------

    @Test
    public void testWorkspaceCRUD() throws Exception {

        Workspace testWorkspace = new Workspace();
        testWorkspace.setGuid(new Random().nextLong());
        testWorkspace.setName("A Sample Workspace");
        testWorkspace.setStatus(1);

        DataModel dataModel = initDataModel();

        List<Workspace> workspaces = null;

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(0, workspaces.size());

        dataModel.addWorkspace(testWorkspace);

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(testWorkspace, workspaces.get(0));

        workspaces = dataModel.getWorkspacesWithStatus(1);
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(testWorkspace, workspaces.get(0));

        workspaces = dataModel.getWorkspacesWithStatus(2);
        assertNotNull(workspaces);
        assertEquals(0, workspaces.size());

        Workspace fetchedWorkspace = dataModel.getWorkspace(testWorkspace.getGuid());
        assertEquals(testWorkspace, fetchedWorkspace);

        testWorkspace.setDescription("A Major General");
        dataModel.updateWorkspace(testWorkspace);

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(testWorkspace, workspaces.get(0));

        dataModel.removeWorkspace(testWorkspace.getGuid());

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(0, workspaces.size());
    }

    @Test
    public void testModifyingWorkspaceObjectDoesNotModifyModel() throws Exception {

        Workspace testWorkspace = new Workspace();
        testWorkspace.setGuid(new Random().nextLong());
        testWorkspace.setName("A Sample Workspace");

        DataModel dataModel = initDataModel();

        List<Workspace> workspaces = null;

        dataModel.addWorkspace(testWorkspace);

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(testWorkspace.getGuid(), workspaces.get(0).getGuid());
        assertEquals(testWorkspace.getName(), workspaces.get(0).getName());
        assertEquals(testWorkspace.getDescription(), workspaces.get(0).getDescription());

        testWorkspace.setDescription("A Major General");

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(testWorkspace.getGuid(), workspaces.get(0).getGuid());
        assertEquals(testWorkspace.getName(), workspaces.get(0).getName());
        assertNotEquals(testWorkspace.getDescription(), workspaces.get(0).getDescription());

        Workspace fetchedWorkspace = dataModel.getWorkspace(testWorkspace.getGuid());
        fetchedWorkspace.setDescription("Do Not Change Model");

        workspaces = dataModel.getWorkspaces();
        assertNotNull(workspaces);
        assertEquals(1, workspaces.size());
        assertEquals(fetchedWorkspace.getGuid(), workspaces.get(0).getGuid());
        assertEquals(fetchedWorkspace.getName(), workspaces.get(0).getName());
        assertNotEquals(fetchedWorkspace.getDescription(), workspaces.get(0).getDescription());
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testFetchNonExistentWorkspace() throws Exception {

        long fakeID = 12345L;
        DataModel dataModel = initDataModel();

        // Expecting NoSuchWorkspaceException
        Workspace workspace = dataModel.getWorkspace(fakeID);
    }

    @Test(expected = WorkspaceAlreadyExistsException.class)
    public void testCreateDuplicateWorkspace() throws Exception {

        Workspace testWorkspace = new Workspace();
        testWorkspace.setGuid(new Random().nextLong());
        testWorkspace.setName("A Sample Workspace");

        DataModel dataModel = initDataModel();

        List<Workspace> workspaces = null;

        dataModel.addWorkspace(testWorkspace);

        // Expecting WorkspaceAlreadyExistsException
        dataModel.addWorkspace(testWorkspace);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testUpdateNonExistentWorkspace() throws Exception {
        Workspace testWorkspace = new Workspace();
        testWorkspace.setGuid(new Random().nextLong());
        testWorkspace.setName("A Sample Workspace");

        DataModel dataModel = initDataModel();

        List<Workspace> workspaces = null;

        // Expecting NoSuchWorkspaceException
        dataModel.updateWorkspace(testWorkspace);
    }

    @Test(expected = NoSuchWorkspaceException.class)
    public void testRemoveNonExistentWorkspace() throws Exception {
        long fakeID = new Random().nextLong();

        DataModel dataModel = initDataModel();

        // Expecting NoSuchWorkspaceException
        dataModel.removeWorkspace(fakeID);
    }

}
