package org.example.vpnserverdatagram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Service
public class Server {

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(51888);

        byte[] buffer = new byte[1024];

        System.out.println("Server started on UDP port 51888");

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);

            String text = new String(packet.getData(), 0, packet.getLength());

            System.out.println("Received: " + text);

            byte[] response = "PONG".getBytes();

            DatagramPacket responsePacket = new DatagramPacket(
                    response,
                    response.length,
                    packet.getAddress(),
                    packet.getPort()
            );

            socket.send(responsePacket);
        }
    }
}
