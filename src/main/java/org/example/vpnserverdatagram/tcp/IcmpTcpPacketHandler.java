package org.example.vpnserverdatagram.tcp;

public class IcmpTcpPacketHandler {

    private static final int IPV4_VERSION = 4;
    private static final int ICMP_PROTOCOL = 1;
    private static final int ICMP_ECHO_REQUEST = 8;
    private static final int ICMP_ECHO_REPLY = 0;
    private static final byte[] SERVER_IP = ip(10, 8, 0, 1);
    private static final byte[] CLIENT_IP = ip(10, 8, 0, 2);

    public byte[] handle(byte[] packet) {
        printAnyIcmp("SERVER VPN ICMP", packet);

        if (!isIcmpEchoRequestToServer(packet)) {
            return packet;
        }

        byte[] response = packet.clone();

        copyIp(response, SERVER_IP, 12);
        copyIp(response, CLIENT_IP, 16);

        int headerLength = (response[0] & 0x0F) * 4;
        response[headerLength] = ICMP_ECHO_REPLY;
        response[headerLength + 2] = 0;
        response[headerLength + 3] = 0;

        int icmpLength = response.length - headerLength;
        int icmpChecksum = checksum(response, headerLength, icmpLength);
        response[headerLength + 2] = (byte) (icmpChecksum >> 8);
        response[headerLength + 3] = (byte) icmpChecksum;

        response[10] = 0;
        response[11] = 0;
        int ipChecksum = checksum(response, 0, headerLength);
        response[10] = (byte) (ipChecksum >> 8);
        response[11] = (byte) ipChecksum;

        printAnyIcmp("SERVER VPN REPLY", response);
        return response;
    }

    private boolean isIcmpEchoRequestToServer(byte[] packet) {
        if (packet.length < 28) {
            return false;
        }

        int version = (packet[0] >> 4) & 0x0F;
        int headerLength = (packet[0] & 0x0F) * 4;

        if (version != IPV4_VERSION) {
            return false;
        }

        if (headerLength < 20 || packet.length < headerLength + 8) {
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

    private static void printAnyIcmp(String prefix, byte[] packet) {
        if (packet.length < 28) {
            return;
        }

        int version = (packet[0] >> 4) & 0x0F;
        int headerLength = (packet[0] & 0x0F) * 4;
        int protocol = packet[9] & 0xFF;

        if (version != IPV4_VERSION || protocol != ICMP_PROTOCOL || packet.length < headerLength + 8) {
            return;
        }

        int icmpType = packet[headerLength] & 0xFF;
        int icmpId = ((packet[headerLength + 4] & 0xFF) << 8) | (packet[headerLength + 5] & 0xFF);
        int icmpSeq = ((packet[headerLength + 6] & 0xFF) << 8) | (packet[headerLength + 7] & 0xFF);

        System.out.println(prefix
                + ": src=" + ipToString(packet, 12)
                + ", dst=" + ipToString(packet, 16)
                + ", type=" + icmpType
                + ", id=" + icmpId
                + ", seq=" + icmpSeq
                + ", size=" + packet.length);
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

    private static String ipToString(byte[] packet, int offset) {
        return (packet[offset] & 0xFF) + "."
                + (packet[offset + 1] & 0xFF) + "."
                + (packet[offset + 2] & 0xFF) + "."
                + (packet[offset + 3] & 0xFF);
    }

    private static byte[] ip(int a, int b, int c, int d) {
        return new byte[]{(byte) a, (byte) b, (byte) c, (byte) d};
    }
}
