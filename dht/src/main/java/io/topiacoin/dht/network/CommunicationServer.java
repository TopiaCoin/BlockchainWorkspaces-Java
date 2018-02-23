package io.topiacoin.dht.network;

import io.topiacoin.dht.MessageSigner;
import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.MessageFactory;
import org.apache.commons.codec.binary.Hex;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyPair;
import java.security.Signature;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class CommunicationServer {

    private static final int BUFFER_SIZE = 64 * 1024; // 64KB UDP Buffer

    private transient boolean isRunning = false;
    private final DatagramSocket socket;
    private final DefaultConfiguration configuration;
    private transient Thread listenerThread;
    private transient Timer timer;
    private transient Map<Integer, ResponseHandler> handlers;
    private transient Map<Integer, TimerTask> tasks;
    private transient Random random;
    private transient MessageFactory messageFactory;
    private transient KeyPair keyPair;
    private transient MessageSigner _messageSigner;

    public CommunicationServer(int udpPort, DefaultConfiguration config, MessageFactory messageFactory, KeyPair keyPair, MessageSigner messageSigner) throws SocketException {

        this.socket = new DatagramSocket(udpPort);
        this.configuration = config;
        this.keyPair = keyPair;

        this.handlers = new HashMap<Integer, ResponseHandler>();
        this.tasks = new HashMap<Integer, TimerTask>();
        this.random = new Random();
        this.timer = new Timer();
        this.messageFactory = messageFactory;
        this._messageSigner = messageSigner;

        this.start();
    }

    private void start() {
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

    private void listen() {
        while (this.isRunning) {
            try {
                byte[] buffer = new byte[BUFFER_SIZE];

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(packet);

                // Decode the received data
                ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                packetBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
                int sigLength = packetBuffer.getInt();
                byte[] signature = new byte[sigLength];
                packetBuffer.get(signature) ;

                System.out.println ( "public key: " + Arrays.toString(keyPair.getPublic().getEncoded()));
                System.out.println ( "signature length: " + signature.length);
                System.out.println ( "packet buffer.length with Signature: " + packetBuffer.limit() ) ;

                System.out.println ("packet buffer: " + Arrays.toString(packetBuffer.array())) ;

                packetBuffer.mark();
                if ( this._messageSigner.verify(packetBuffer, this.keyPair, signature) ) {
                    packetBuffer.reset();

                    int messageLength = packetBuffer.getInt();
                    int msgID = packetBuffer.getInt();
                    byte msgType = packetBuffer.get();

                    Message message = this.messageFactory.createMessage(msgType, packetBuffer);

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
                        responseHandler = messageFactory.createReceiver(msgType, this);
                    }

                    if (responseHandler != null) {
                        responseHandler.receive(message, msgID);
                    }
                } else {
                    System.err.println("Signature Verification Failed!");
                }

            } catch (IOException e) {
                // Failed to read the data.  Move along
            }
        }
    }

    public void sendMessage(Node recipient, Message message, ResponseHandler responseHandler) {
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
                this.timer.schedule(timerTask, this.configuration.getResponseTimeout());
                this.tasks.put(msgID, timerTask);
            } catch (IllegalStateException e) {
                // The timer must have been cancelled.  Ignore and proceed.
            }
        }

        // Send the message to the recipient.
        sendMessage(recipient, message, msgID);
    }

    public void reply(Node recipient, Message message, int msgID) {
        if (!this.isRunning) {
            throw new IllegalStateException("The Communication Server is not running");
        }
        sendMessage(recipient, message, msgID);
    }

    private void sendMessage(Node recipient, Message message, int msgID) {

        // Encode the message for sending
        ByteBuffer messageBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        messageBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
        messageBuffer.putInt(msgID) ;
        messageBuffer.put(message.getType());
        message.encodeMessage(messageBuffer);
        messageBuffer.flip();

        System.out.println ( "message buffer.length with Signature: " + messageBuffer.limit() ) ;
        System.out.println ("message buffer: " + Arrays.toString(messageBuffer.array())) ;

        // Sign the Message and append the signature
        byte[]signature = this._messageSigner.sign(messageBuffer, this.keyPair) ;
        messageBuffer.flip();

        ByteBuffer packetBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        packetBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
        packetBuffer.putInt(signature.length);
        packetBuffer.put(signature);
        packetBuffer.putInt(messageBuffer.remaining());
        packetBuffer.put(messageBuffer);
        packetBuffer.flip();

        System.out.println ( "signature length: " + signature.length);
        System.out.println("Signature: " + Hex.encodeHexString(signature));
        System.out.println ( "packet buffer.length with Signature: " + packetBuffer.limit() ) ;

        System.out.println ("packet buffer: " + Arrays.toString(packetBuffer.array())) ;


        // Build the Datagram Packet
        DatagramPacket packet = new DatagramPacket(packetBuffer.array(), 0, packetBuffer.limit()) ;
        // TODO: Set the Socket Address of the packet so that it will get delivered
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
