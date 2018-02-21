package io.topiacoin.dht.network;

import io.topiacoin.dht.config.DefaultConfiguration;
import io.topiacoin.dht.intf.Message;
import io.topiacoin.dht.intf.ResponseHandler;
import io.topiacoin.dht.messages.MessageFactory;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    public CommunicationServer(int udpPort, DefaultConfiguration config, MessageFactory messageFactory) throws SocketException {

        this.socket = new DatagramSocket(udpPort);
        this.configuration = config;

        this.handlers = new HashMap<Integer, ResponseHandler>();
        this.tasks = new HashMap<Integer, TimerTask>();
        this.random = new Random();
        this.timer = new Timer();
        this.messageFactory = messageFactory;

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
                ByteBuffer byteBuffer = ByteBuffer.wrap(packet.getData(), 0, packet.getLength());
                byteBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
                int msgID = byteBuffer.getInt();
                byte msgType = byteBuffer.get();

                Message message = this.messageFactory.createMessage(msgType, byteBuffer);

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

            } catch (IOException e) {
                // Failed to read the data.  Move along
            }
        }
    }

    public void sendMessage(Object recipient, Message message, ResponseHandler responseHandler) {
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

    public void reply(Object recipient, Message message, int msgID) {
        if (!this.isRunning) {
            throw new IllegalStateException("The Communication Server is not running");
        }
        sendMessage(recipient, message, msgID);
    }

    private void sendMessage(Object recipient, Message message, int msgID) {

        // Encode the message for sending
        ByteBuffer byteBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        byteBuffer.order(ByteOrder.BIG_ENDIAN); // Network Byte Order
        byteBuffer.putInt(msgID) ;
        byteBuffer.put(message.getType());
        message.encodeMessage(byteBuffer);

        byteBuffer.flip();

        byte[] data = new byte[byteBuffer.limit()] ;
        byteBuffer.get(data) ;

        DatagramPacket packet = new DatagramPacket(data, 0, data.length) ;
        // TODO: Set the Socket Address of the packet so that it will get delivered
//        packet.setSocketAddress(recipient.getSocketAddress());

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
