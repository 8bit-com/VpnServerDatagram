package org.example.vpnserverdatagram.vpn;

public class Ipv4Address {

    public static int of(String address) {
        String[] parts = address.split("\\.");

        return (Integer.parseInt(parts[0]) << 24) |
                (Integer.parseInt(parts[1]) << 16) |
                (Integer.parseInt(parts[2]) << 8) |
                Integer.parseInt(parts[3]);
    }

    public static int read(byte[] packet, int offset) {
        return ((packet[offset] & 0xFF) << 24) |
                ((packet[offset + 1] & 0xFF) << 16) |
                ((packet[offset + 2] & 0xFF) << 8) |
                (packet[offset + 3] & 0xFF);
    }

    public static void write(byte[] packet, int offset, int address) {
        packet[offset] = (byte) ((address >>> 24) & 0xFF);
        packet[offset + 1] = (byte) ((address >>> 16) & 0xFF);
        packet[offset + 2] = (byte) ((address >>> 8) & 0xFF);
        packet[offset + 3] = (byte) (address & 0xFF);
    }
}
