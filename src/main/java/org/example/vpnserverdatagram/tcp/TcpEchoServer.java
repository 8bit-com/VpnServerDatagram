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
                socket.setTcpNoDelay(true);
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
                    System.out.println("TCP BAD FRAME SIZE: type=" + frameType + ", size=" + size + ", client=" + socket.getRemoteSocketAddress());
                    return;
                }

                byte[] data = input.readNBytes(size);

                if (data.length != size) {
                    System.out.println("TCP SHORT FRAME: expected=" + size + ", actual=" + data.length + ", client=" + socket.getRemoteSocketAddress());
                    return;
                }

                if (frameType == FRAME_TEST) {
                    output.writeByte(frameType);
                    output.writeInt(data.length);
                    output.write(data);
                    output.flush();
                    System.out.println("TCP TEST ECHO: size=" + size + ", client=" + socket.getRemoteSocketAddress());
                    continue;
                }

                if (frameType == FRAME_VPN) {
                    byte[] response = icmpTcpPacketHandler.handle(data);
                    if (response != null) {
                        output.writeByte(frameType);
                        output.writeInt(response.length);
                        output.write(response);
                        output.flush();
                    }
                    continue;
                }

                System.out.println("TCP UNKNOWN FRAME: type=" + frameType + ", size=" + size + ", client=" + socket.getRemoteSocketAddress());
                return;
            }
        } catch (Exception e) {
            System.out.println("TCP CLIENT CLOSED OR ERROR: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
