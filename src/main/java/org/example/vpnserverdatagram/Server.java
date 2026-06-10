package org.example.vpnserverdatagram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class Server {

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(51888);

        socket.setReceiveBufferSize(16 * 1024 * 1024);
        socket.setSendBufferSize(16 * 1024 * 1024);

        ExecutorService sendPool = Executors.newFixedThreadPool(8);

        while (true) {
            byte[] buf = new byte[2048];
            DatagramPacket p = new DatagramPacket(buf, buf.length);

            socket.receive(p);

            byte[] data = p.getData();
            int length = p.getLength();
            InetAddress address = p.getAddress();
            int port = p.getPort();

            sendPool.execute(() -> {
                try {
                    socket.send(new DatagramPacket(
                            data,
                            length,
                            address,
                            port
                    ));
                } catch (Exception ignored) {
                }
            });
        }
    }
}
