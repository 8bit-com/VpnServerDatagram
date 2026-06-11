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
public class TcpVpnServer {

    private static final int PORT = 51891;
    private static final int MAX_PACKET_SIZE = 2048;
    private static final int WORKER_THREADS = 8;

    private final ExecutorService workers = Executors.newFixedThreadPool(WORKER_THREADS);
    private final IcmpTcpPacketHandler icmpTcpPacketHandler = new IcmpTcpPacketHandler();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread thread = new Thread(this::run, "tcp-vpn-server");
        thread.start();
    }

    private void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("TCP VPN server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("TCP VPN client connected: " + socket.getRemoteSocketAddress());
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
                int size = input.readInt();

                if (size <= 0 || size > MAX_PACKET_SIZE) {
                    return;
                }

                byte[] data = input.readNBytes(size);

                if (data.length != size) {
                    return;
                }

                byte[] response = icmpTcpPacketHandler.handle(data);

                output.writeInt(response.length);
                output.write(response);
                output.flush();
            }
        } catch (Exception e) {
            System.out.println("TCP VPN client disconnected: " + e.getMessage());
        }
    }
}
