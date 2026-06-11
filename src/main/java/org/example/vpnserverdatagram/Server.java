package org.example.vpnserverdatagram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.Objects.isNull;

@Service
public class Server {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        ExecutorService sendPool = Executors.newFixedThreadPool(8);
        DatagramSocket socket = getSocket();
        System.out.println("Server started on UDP port 51888");

        while (!isNull(socket)) {
            DatagramPacket packet = getRequestPacket();

            byte[] data = receive(socket, packet);

            sendPool.execute(() -> send(socket, packet, data));
        }
    }

    private DatagramPacket getRequestPacket() {
        byte[] buffer = new byte[1024];
        return new DatagramPacket(buffer, buffer.length);
    }

    private DatagramSocket getSocket() {
        try {
            return new DatagramSocket(51888);
        } catch (SocketException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    private byte[] receive(DatagramSocket socket, DatagramPacket packet) {
        try {
            socket.receive(packet);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        String text = new String(packet.getData(), 0, packet.getLength());
        System.out.println("Received: " + text);
        return packet.getData();
    }

    private void send(DatagramSocket socket, DatagramPacket packet, byte[] response) {
        DatagramPacket responsePacket = getResponsePacket(response,  packet);
        try {
            socket.send(responsePacket);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private DatagramPacket getResponsePacket(byte[] response, DatagramPacket packet) {
        return new DatagramPacket(
                response,
                response.length,
                packet.getAddress(),
                packet.getPort());
    }
}
