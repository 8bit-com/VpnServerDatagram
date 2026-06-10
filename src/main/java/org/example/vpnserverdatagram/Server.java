package org.example.vpnserverdatagram;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class Server {

    private static final int PORT = 51888;
    private static final int BUFFER_SIZE = 2048;
    private static final int SOCKET_BUFFER_SIZE = 16 * 1024 * 1024;
    private static final int SEND_THREADS = 8;

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(PORT);

        socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
        socket.setSendBufferSize(SOCKET_BUFFER_SIZE);

        ExecutorService sendPool = Executors.newFixedThreadPool(SEND_THREADS);

        System.out.println("UDP echo server started on port " + PORT);

        while (true) {
            DatagramPacket request = receive(socket);
            byte[] data = copy(request);

            sendPool.execute(() -> send(socket, data, request));
        }
    }

    private DatagramPacket receive(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        socket.receive(packet);
        return packet;
    }

    private byte[] copy(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    private void send(DatagramSocket socket, byte[] data, DatagramPacket request) {
        try {
            socket.send(new DatagramPacket(
                    data,
                    data.length,
                    request.getAddress(),
                    request.getPort()
            ));
        } catch (Exception ignored) {
        }
    }
}
