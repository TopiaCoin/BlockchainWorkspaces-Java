package io.topiacoin.chainmail;

import io.topiacoin.chainmail.exceptions.NoSuchChainException;

public class Chainmail {

    /**
     * Posted when a blockchain is started by chainmail The classifier of this notification will be the
     * workspaceGUID.The notification will not have any info.
     */
    private static final String BLOCKCHAIN_STARTED = "blockchainStarted";


    /**
     * Posted when a blockchain is stopped by chainmail The classifier of this notification will be the
     * workspaceGUID.The notification will not have any info.
     */
    private static final String BLOCKCHAIN_STOPPED = "blockchainStopped";

    /**
     * Constructs a Chainmail instance. Expects Spring-like configuration of certain variables (such as the install
     * location of Multichain and the Port Range)
     */
    public Chainmail() {

    }

    /**
     * Starts the blockchain for the specified workspaceId. If the chain doesn't exist, creates it. Will stop the
     * least-active blockchain currently running if the blockchain pool is full.
     * <p>
     * Upon successful startup of the blockchain, a notification of type "blockchainStarted" will be posted to the
     * notification center. The notification classifier will be the workspaceID of the blockchain.
     *
     * @param workspaceId
     *
     * @throws NoSuchChainException
     */
    public void startBlockchain(String workspaceId) throws NoSuchChainException {

    }

    /**
     * Starts the blockchain for the specified workspaceId by connecting to the peer specified in the
     * perrConnectionString.  Will stop the least-active blockchain currently running if the blockchain pool is full. If
     * the workspace doesn't exist, and the peer cannot be reached, a NoSuchChainException is thrown.
     * <p>
     * Upon successful startup of the blockchain, a notification of type "blockchainStarted" will be posted to the
     * notification center. The notification classifier will be the workspaceID of the blockchain.
     *
     * @param workspaceId
     * @param peerConnectionString
     *
     * @throws NoSuchChainException
     */
    public void startBlockchain(String workspaceId, String peerConnectionString) throws NoSuchChainException {

    }

    /**
     * Stops all running blockchains.
     * <p>
     * Notifications of type "blockchainStopped" will be posted to the notification center as each blockchain is
     * stopped. The notification classifier will be the ID of the blockchain that was stopped.
     */
    public void stopAllChains() {

    }

    /**
     * Stops the blockchain for the specified workspaceId, and removes it from the blockchain pool.
     * <p>
     * Notifications of type "blockchainStopped" will be posted to the notification center when the blockchain is
     * stopped. The notification classifier will be the ID of the blockchain that was stopped.
     *
     * @param workspaceId
     *
     * @throws NoSuchChainException
     */
    public void stopBlockchain(String workspaceId) throws NoSuchChainException {

    }


}
