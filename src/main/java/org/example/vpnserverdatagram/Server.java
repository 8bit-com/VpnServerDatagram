package org.example.vpnserverdatagram;

import org.example.vpnserverdatagram.ip.IcmpEchoReplyBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

@Service
public class Server {

    private static final int PORT = 51888;
    private static final int BUFFER_SIZE = 2048;
    private static final String SERVER_TUN_IP = "10.8.0.1";

    private final IcmpEchoReplyBuilder icmpEchoReplyBuilder = new IcmpEchoReplyBuilder(SERVER_TUN_IP);

    @EventListener(ApplicationReadyEvent.class)
    public void start() throws IOException {
        DatagramSocket socket = new DatagramSocket(PORT);

        socket.setReceiveBufferSize(16 * 1024 * 1024);
        socket.setSendBufferSize(16 * 1024 * 1024);

        System.out.println("UDP server started on port " + PORT);

        while (true) {
            DatagramPacket packet = receive(socket);
            byte[] request = copyPacketData(packet);
            byte[] reply = icmpEchoReplyBuilder.buildReply(request);

            if (reply == null) {
                printPacket(request);
                continue;
            }

            socket.send(new DatagramPacket(
                    reply,
                    reply.length,
                    packet.getAddress(),
                    packet.getPort()
            ));

            System.out.println("SERVER ICMP reply sent, size=" + reply.length);
        }
    }

    private DatagramPacket receive(DatagramSocket socket) throws IOException {
        DatagramPacket packet = new DatagramPacket(new byte[BUFFER_SIZE], BUFFER_SIZE);
        socket.receive(packet);
        return packet;
    }

    private byte[] copyPacketData(DatagramPacket packet) {
        byte[] data = new byte[packet.getLength()];
        System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
        return data;
    }

    private void printPacket(byte[] packet) {
        int version = (packet[0] >>> 4) & 0x0F;

        if (version == 4) {
            System.out.println("SERVER TUN PACKET: IPv4 size=" + packet.length + ", protocol=" + (packet[9] & 0xFF));
            return;
        }

        System.out.println("SERVER TUN PACKET: version=" + version + ", size=" + packet.length);
    }
}
