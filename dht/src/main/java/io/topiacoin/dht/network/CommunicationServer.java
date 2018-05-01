package io.topiacoin.dht.network;

import io.topiacoin.crypto.CryptographicException;
import io.topiacoin.crypto.MessageSigner;
import io.topiacoin.dht.DHTComponents;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.MessageFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CommunicationServer {

    private final Log _log = LogFactory.getLog(this.getClass());

    private static final int BUFFER_SIZE = 64 * 1024; // 64KB UDP Buffer

    private transient boolean isRunning = false;
    private final DatagramSocket socket;
    private transient Thread listenerThread;
    private transient Timer timer;
    private transient Map<Integer, ResponseHandler> handlers;
    private transient Map<Integer, TimerTask> tasks;
    private transient Random random;
    private transient Node node;
    private transient KeyPair keyPair;

    private DHTComponents _dhtComponents;

    public CommunicationServer(int udpPort, KeyPair keyPair, Node node) throws SocketException {

        this.socket = new DatagramSocket(udpPort);
        this.keyPair = keyPair;
        this.node = node ;

        this.handlers = new HashMap<Integer, ResponseHandler>();
        this.tasks = new HashMap<Integer, TimerTask>();
        this.random = new Random();
        this.timer = new Timer("CS-" + udpPort + " Handler Purge Timer");
    }

    public void start() {
        listenerThread = new Thread(new Runnable() {
            public void run() {
                listen();
            }
        });
        this.isRunning = true;
        listenerThread.start();
    }

    public void shutdown() {
        this.isRunning = false;
        this.socket.close();
        this.timer.cancel();
    }

    public int getPort() {
        return this.socket.getLocalPort();
    }

    private void listen() {
        NodeIDGenerator nodeIDGenerator = new NodeIDGenerator(_dhtComponents.getConfiguration());
        while (this.isRunning) {
            try {
                MessageSigner messageSigner = _dhtComponents.getMessageSigner();
                MessageFactory messageFactory = _dhtComponents.getMessageFactory();

                byte[] buffer = new byte[BUFFER_SIZE];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);

                // Decode the received data
                ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                packetBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
                int sigLength = packetBuffer.getInt();
                byte[] signature = new byte[sigLength];
                packetBuffer.get(signature) ;
                int pubKeyLength = packetBuffer.getInt();
                byte[] pubKey = new byte[pubKeyLength];
                packetBuffer.get(pubKey);

                // TODO Reconstruct the Public Key from the Buffer

                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                PublicKey publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(pubKey)) ;

//                System.out.println ( "public key: " + Arrays.toString(keyPair.getPublic().getEncoded()));
//                System.out.println ( "signature length: " + signature.length);
//                System.out.println ( "packet buffer.length with Signature: " + packetBuffer.limit() ) ;
//
//                System.out.println ("packet buffer: " + Arrays.toString(packetBuffer.array())) ;

                int messageLength = packetBuffer.getInt();

                packetBuffer.mark();
                if ( messageSigner.verify(packetBuffer, publicKey, signature) ) {
                    packetBuffer.reset();

                    byte[] nodeIDBytes = new byte[20] ;
                    byte[] validationBytes = new byte[20] ;
                    packetBuffer.get(nodeIDBytes);
                    packetBuffer.get(validationBytes);

                    NodeID originNodeID = new NodeID(nodeIDBytes, validationBytes) ;
                    if ( !nodeIDGenerator.validateNodeID(originNodeID)) {
                        _log.warn ( "Ignoring a message with an Invalid Node ID: " + originNodeID);
                        continue;
                    }
                    Node originNode = new Node(originNodeID, packet.getAddress(), packet.getPort()) ;

                    int msgID = packetBuffer.getInt();
                    byte msgType = packetBuffer.get();

                    Message message = messageFactory.createMessage(msgType, packetBuffer);

                    _log.debug ( "Received Message: " + originNode.getPort() + "->" + this.socket.getLocalPort() + " -- " + Integer.toString(msgID, 16) + " -- " + message) ;

                    // Find the Response Handler
                    ResponseHandler responseHandler = null;
                    if (this.handlers.containsKey(msgID)) {
                        synchronized (this) {
                            responseHandler = this.handlers.remove(msgID);
                            TimerTask timerTask = this.tasks.remove(msgID);
                            if (timerTask != null) {
                                timerTask.cancel();
                            }
                        }
                    } else {
                        responseHandler = messageFactory.createReceiver(msgType);
                    }

                    if (responseHandler != null) {
                        responseHandler.receive(originNode, message, msgID);
                    }
                } else {
                    System.err.println("Signature Verification Failed!");
                }

            } catch (IOException e) {
                // Failed to read the data.  Move along
            } catch (CryptographicException e) {
                // Failed to verify the data.  Move along
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }
    }

    public int sendMessage(Node recipient, Message message, ResponseHandler responseHandler) {
        if (!this.isRunning) {
            throw new IllegalStateException("The Communication Server is not running");
        }

        // Find a message ID for the message we are about to send.
        // Make sure we don't collide with an existing message ID.
        int msgID = 0;
        while (msgID == 0 || this.handlers.containsKey(msgID)) {
            msgID = random.nextInt();
        }

        if (responseHandler != null) {
            try {
                // Save the Response Handler and schedule the timeout task
                handlers.put(msgID, responseHandler);
                TimerTask timerTask = new TimeoutTask(msgID, responseHandler);
                this.timer.schedule(timerTask, this._dhtComponents.getConfiguration().getResponseTimeout());
                this.tasks.put(msgID, timerTask);
            } catch (IllegalStateException e) {
                // The timer must have been cancelled.  Ignore and proceed.
            }
        }

        // Send the message to the recipient.
        sendMessage(recipient, message, msgID);

        return msgID;
    }

    public void reply(Node recipient, Message message, int msgID) {
        if (!this.isRunning) {
            throw new IllegalStateException("The Communication Server is not running");
        }
        sendMessage(recipient, message, msgID);
    }

    private void sendMessage(Node recipient, Message message, int msgID) {

        MessageSigner messageSigner = _dhtComponents.getMessageSigner();

        _log.debug ( "Sending Message:  " + this.socket.getLocalPort() + "->" + recipient.getPort() + " -- " + Integer.toString(msgID, 16) + " -- " + message) ;

        // Encode the message for sending
        ByteBuffer messageBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        messageBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
        messageBuffer.put(this.node.getNodeID().getNodeID());
        messageBuffer.put(this.node.getNodeID().getValidation());
        messageBuffer.putInt(msgID) ;
        messageBuffer.put(message.getType());
        message.encodeMessage(messageBuffer);
        messageBuffer.flip();

//        System.out.println ( "message buffer.length with Signature: " + messageBuffer.limit() ) ;
//        System.out.println ("message buffer: " + Arrays.toString(messageBuffer.array())) ;

        // Sign the Message and append the signature
        byte[] signature = new byte[0];
        try {
            signature = messageSigner.sign(messageBuffer, this.keyPair);
            messageBuffer.flip();
        } catch ( CryptographicException e ) {
            _log.warn ( "Unable to generate signature for Message", e) ;
        }

        byte[] publicKey = this.keyPair.getPublic().getEncoded();

        ByteBuffer packetBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        packetBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
        packetBuffer.putInt(signature.length);
        packetBuffer.put(signature);
        packetBuffer.putInt(publicKey.length);
        packetBuffer.put(publicKey);
        packetBuffer.putInt(messageBuffer.remaining());
        packetBuffer.put(messageBuffer);
        packetBuffer.flip();

//        System.out.println ( "signature length: " + signature.length);
//        System.out.println("Signature: " + Hex.encodeHexString(signature));
//        System.out.println ( "packet buffer.length with Signature: " + packetBuffer.limit() ) ;
//
//        System.out.println ("packet buffer: " + Arrays.toString(packetBuffer.array())) ;


        // Build the Datagram Packet
        DatagramPacket packet = new DatagramPacket(packetBuffer.array(), 0, packetBuffer.limit()) ;
        packet.setSocketAddress(recipient.getSocketAddress());

        try {
            this.socket.send(packet);
        } catch (IOException e) {
            // Failed to send the packet

        }
    }

    private void unregister(int msgID) {
        this.handlers.remove(msgID);
        this.tasks.remove(msgID);

    }

    public void setDHTComponents(DHTComponents dhtComponents) {
        _dhtComponents = dhtComponents;
    }

    private class TimeoutTask extends TimerTask {

        private final int msgID;
        private final ResponseHandler responseHandler;

        public TimeoutTask(int msgID, ResponseHandler responseHandler) {
            this.msgID = msgID;
            this.responseHandler = responseHandler;
        }

        /**
         * The action to be performed by this timer task.
         */
        public void run() {
            if (!CommunicationServer.this.isRunning) {
                return;
            }

            // Find the responseHandler for the
            try {
                unregister(msgID);
                responseHandler.timeout(msgID);
            } catch (Exception e) {
                // NOOP
            }
        }
    }
}
