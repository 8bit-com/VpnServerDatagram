package org.example.vpnserverdatagram.tcp;

public class IcmpTcpPacketHandler {

    private static final int IPV4_VERSION = 4;
    private static final int ICMP_PROTOCOL = 1;
    private static final int ICMP_ECHO_REQUEST = 8;
    private static final int ICMP_ECHO_REPLY = 0;
    private static final byte[] SERVER_IP = ip(10, 8, 0, 1);
    private static final byte[] CLIENT_IP = ip(10, 8, 0, 2);

    public byte[] handle(byte[] packet) {
        if (!isIcmpEchoRequestToServer(packet)) {
            return packet;
        }

        byte[] response = packet.clone();

        copyIp(response, CLIENT_IP, 12);
        copyIp(response, SERVER_IP, 16);

        response[20] = ICMP_ECHO_REPLY;
        response[22] = 0;
        response[23] = 0;

        int icmpLength = response.length - 20;
        int icmpChecksum = checksum(response, 20, icmpLength);
        response[22] = (byte) (icmpChecksum >> 8);
        response[23] = (byte) icmpChecksum;

        response[10] = 0;
        response[11] = 0;
        int ipChecksum = checksum(response, 0, 20);
        response[10] = (byte) (ipChecksum >> 8);
        response[11] = (byte) ipChecksum;

        System.out.println("ICMP echo reply created");
        return response;
    }

    private boolean isIcmpEchoRequestToServer(byte[] packet) {
        if (packet.length < 28) {
            return false;
        }

        int version = (packet[0] >> 4) & 0x0F;
        int headerLength = (packet[0] & 0x0F) * 4;

        if (version != IPV4_VERSION || headerLength != 20) {
            return false;
        }

        int protocol = packet[9] & 0xFF;
        if (protocol != ICMP_PROTOCOL) {
            return false;
        }

        if (!ipEquals(packet, 16, SERVER_IP)) {
            return false;
        }

        int icmpType = packet[headerLength] & 0xFF;
        return icmpType == ICMP_ECHO_REQUEST;
    }

    private static int checksum(byte[] data, int offset, int length) {
        long sum = 0;
        int index = offset;

        while (length > 1) {
            sum += ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
            index += 2;
            length -= 2;
        }

        if (length > 0) {
            sum += (data[index] & 0xFF) << 8;
        }

        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return (int) (~sum) & 0xFFFF;
    }

    private static boolean ipEquals(byte[] packet, int offset, byte[] ip) {
        return packet[offset] == ip[0]
                && packet[offset + 1] == ip[1]
                && packet[offset + 2] == ip[2]
                && packet[offset + 3] == ip[3];
    }

    private static void copyIp(byte[] packet, byte[] ip, int offset) {
        packet[offset] = ip[0];
        packet[offset + 1] = ip[1];
        packet[offset + 2] = ip[2];
        packet[offset + 3] = ip[3];
    }

    private static byte[] ip(int a, int b, int c, int d) {
        return new byte[]{(byte) a, (byte) b, (byte) c, (byte) d};
    }
}
