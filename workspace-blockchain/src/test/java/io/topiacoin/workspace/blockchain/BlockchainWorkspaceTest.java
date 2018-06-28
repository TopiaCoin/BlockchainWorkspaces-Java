package io.topiacoin.workspace.blockchain;

import io.topiacoin.core.Configuration;
import io.topiacoin.core.callbacks.CreateWorkspaceCallback;
import io.topiacoin.core.exceptions.NotLoggedInException;
import io.topiacoin.core.impl.DefaultConfiguration;
import org.junit.Test;

public class BlockchainWorkspaceTest {

	@Test
	public void theGreatestIntegrationTestTheWorldHasEverSeen() throws NotLoggedInException {
		Configuration config = new DefaultConfiguration();
		BlockchainWorkspace workspace = new BlockchainWorkspace(config);

		workspace.createWorkspace("A Workspace", "A description", new CreateWorkspaceCallback() {
			@Override public void createdWorkspace(long workspaceID) {

			}

			@Override public void failedToCreateWorkspace() {

			}
		});
	}

}
