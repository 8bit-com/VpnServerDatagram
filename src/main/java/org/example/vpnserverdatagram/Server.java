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

        System.out.println("UDP echo server started on port 51888");

        byte[] buffer = new byte[2048];

        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);

            socket.send(new DatagramPacket(
                    packet.getData(),
                    packet.getLength(),
                    packet.getAddress(),
                    packet.getPort()
            ));
        }
    }

}
