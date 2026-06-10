package org.example.vpnserverdatagram.vpn;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class VpnPingServer {

    private static final int PORT = 51889;
    private static final int BUFFER_SIZE = 2048;
    private static final int SOCKET_BUFFER_SIZE = 16 * 1024 * 1024;
    private static final String SERVER_TUN_IP = "10.8.0.1";

    private final IcmpEchoReplyBuilder icmpEchoReplyBuilder = new IcmpEchoReplyBuilder(SERVER_TUN_IP);

    public void start() {
        Thread thread = new Thread(this::run, "vpn-ping-server");
        thread.start();
    }

    private void run() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            socket.setReceiveBufferSize(SOCKET_BUFFER_SIZE);
            socket.setSendBufferSize(SOCKET_BUFFER_SIZE);

            System.out.println("VPN ping server started on port " + PORT);

            while (true) {
                DatagramPacket request = receive(socket);
                byte[] reply = icmpEchoReplyBuilder.buildReply(copy(request));

                if (reply == null) {
                    continue;
                }

                socket.send(new DatagramPacket(
                        reply,
                        reply.length,
                        request.getAddress(),
                        request.getPort()
                ));

                System.out.println("VPN ICMP reply sent, size=" + reply.length);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DatagramPacket receive(DatagramSocket socket) throws Exception {
        DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        socket.receive(packet);
        return packet;
    }

    private byte[] copy(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }
}
