package net.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SocketThread extends Thread {

    private final SocketThreadListener listener;
    private final Socket socket;
    private DataOutputStream out;

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.socket = socket;
        this.listener = listener;
        start();
    }

    @Override
    public void run() {
        try {
            listener.onSocketStart(this, socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketReady(this, socket);
            while (!isInterrupted()) {
                int length = in.read();
                if(length > 0) {
                    byte[] bytemsg = new byte[length];
                    for (int i = 0; i < length; i++) {
                        bytemsg[i] = in.readByte();
                    }
                    String msg = new String(bytemsg);
                    listener.onReceiveString(this, socket, msg);
                } else{
                    System.out.println(length);
                }
            }
        } catch (IOException e) {
            listener.onSocketException(this, e);
        } finally {
            close();
            listener.onSocketStop(this);
        }
    }

    public synchronized boolean sendMessage(String msg) {
        try {
            String encodedString = Base64.getEncoder().encodeToString(msg.getBytes());
            byte[] bytes = Base64.getDecoder().decode(encodedString);
            out.write(bytes.length);
            for(int i =0; i < bytes.length; i++) {
                out.writeByte(bytes[i]);
            }
            out.flush();
            return true;
        } catch (IOException e) {
            listener.onSocketException(this, e);
            close();
            return false;
        }
    }

    public synchronized void close() {
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onSocketException(this, e);
        }
    }

}