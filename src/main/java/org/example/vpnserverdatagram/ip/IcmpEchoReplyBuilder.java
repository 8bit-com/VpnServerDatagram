package org.example.vpnserverdatagram.ip;

import java.util.Arrays;

public class IcmpEchoReplyBuilder {

    private static final int IPV4_VERSION = 4;
    private static final int ICMP_PROTOCOL = 1;
    private static final int ICMP_ECHO_REQUEST = 8;
    private static final int ICMP_ECHO_REPLY = 0;

    private final int localAddress;

    public IcmpEchoReplyBuilder(String localAddress) {
        this.localAddress = Ipv4Address.of(localAddress);
    }

    public byte[] buildReply(byte[] request) {
        if (!isEchoRequestToLocalAddress(request)) {
            return null;
        }

        byte[] reply = Arrays.copyOf(request, request.length);

        int ipHeaderLength = ipHeaderLength(reply);
        int sourceAddress = Ipv4Address.read(reply, 12);
        int destinationAddress = Ipv4Address.read(reply, 16);

        Ipv4Address.write(reply, 12, destinationAddress);
        Ipv4Address.write(reply, 16, sourceAddress);

        reply[ipHeaderLength] = ICMP_ECHO_REPLY;
        reply[ipHeaderLength + 2] = 0;
        reply[ipHeaderLength + 3] = 0;
        writeChecksum(reply, ipHeaderLength + 2, Checksum.calculate(reply, ipHeaderLength, reply.length - ipHeaderLength));

        reply[10] = 0;
        reply[11] = 0;
        writeChecksum(reply, 10, Checksum.calculate(reply, 0, ipHeaderLength));

        return reply;
    }

    private boolean isEchoRequestToLocalAddress(byte[] packet) {
        if (packet.length < 28) {
            return false;
        }

        if (ipVersion(packet) != IPV4_VERSION) {
            return false;
        }

        if ((packet[9] & 0xFF) != ICMP_PROTOCOL) {
            return false;
        }

        int ipHeaderLength = ipHeaderLength(packet);

        if (packet.length <= ipHeaderLength) {
            return false;
        }

        if ((packet[ipHeaderLength] & 0xFF) != ICMP_ECHO_REQUEST) {
            return false;
        }

        return Ipv4Address.read(packet, 16) == localAddress;
    }

    private int ipVersion(byte[] packet) {
        return (packet[0] >>> 4) & 0x0F;
    }

    private int ipHeaderLength(byte[] packet) {
        return (packet[0] & 0x0F) * 4;
    }

    private void writeChecksum(byte[] packet, int offset, short checksum) {
        packet[offset] = (byte) ((checksum >>> 8) & 0xFF);
        packet[offset + 1] = (byte) (checksum & 0xFF);
    }
}
