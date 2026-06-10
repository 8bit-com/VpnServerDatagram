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

        socket.setReceiveBufferSize(16 * 1024 * 1024);
        socket.setSendBufferSize(16 * 1024 * 1024);

        System.out.println("UDP server started on port 51888");

        while (true) {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.receive(packet);

            byte[] data = copyPacketData(packet);
            printPacket(data);
        }
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
