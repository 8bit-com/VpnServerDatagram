package org.example.vpnserverdatagram.tcp;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TcpEchoServer {

    private static final int PORT = 51890;
    private static final int MAX_PACKET_SIZE = 2048;
    private static final int WORKER_THREADS = 8;
    private static final int FRAME_TEST = 1;
    private static final int FRAME_VPN = 2;

    private final ExecutorService workers = Executors.newFixedThreadPool(WORKER_THREADS);
    private final IcmpTcpPacketHandler icmpTcpPacketHandler = new IcmpTcpPacketHandler();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread thread = new Thread(this::run, "tcp-server");
        thread.start();
    }

    private void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("TCP server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("TCP client connected: " + socket.getRemoteSocketAddress());
                workers.execute(() -> handle(socket));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handle(Socket socket) {
        try (socket;
             DataInputStream input = new DataInputStream(socket.getInputStream());
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            while (true) {
                int frameType = input.readUnsignedByte();
                int size = input.readInt();

                if (size <= 0 || size > MAX_PACKET_SIZE) {
                    return;
                }

                byte[] data = input.readNBytes(size);

                if (data.length != size) {
                    return;
                }

                if (frameType == FRAME_TEST) {
                    output.writeByte(frameType);
                    output.writeInt(data.length);
                    output.write(data);
                    output.flush();
                    System.out.println("TCP TEST ECHO: size=" + size + ", client=" + socket.getRemoteSocketAddress());
                } else if (frameType == FRAME_VPN) {
                    System.out.println("TCP VPN FRAME: size=" + size + ", client=" + socket.getRemoteSocketAddress());
                    byte[] response = icmpTcpPacketHandler.handle(data);
                    if (response != null) {
                        output.writeByte(frameType);
                        output.writeInt(response.length);
                        output.write(response);
                        output.flush();
                    }
                } else {
                    System.out.println("TCP UNKNOWN FRAME: type=" + frameType + ", size=" + size);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }
}
