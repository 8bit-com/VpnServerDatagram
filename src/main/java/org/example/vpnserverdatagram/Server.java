package org.example.vpnserverdatagram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

@Service
public class Server {

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        int port = 51888;

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("Server started on port " + port);

            byte[] buffer = new byte[2048];

            while (true) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String message = new String(
                        request.getData(),
                        request.getOffset(),
                        request.getLength(),
                        StandardCharsets.UTF_8
                );

                System.out.println("Received from "
                        + request.getAddress().getHostAddress()
                        + ":"
                        + request.getPort()
                        + " -> " + message);

                if ("PING".equals(message)) {
                    byte[] responseData = "PONG".getBytes(StandardCharsets.UTF_8);

                    DatagramPacket response = new DatagramPacket(
                            responseData,
                            responseData.length,
                            request.getAddress(),
                            request.getPort()
                    );

                    socket.send(response);
                    System.out.println("Sent PONG");
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
