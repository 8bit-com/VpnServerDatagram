package org.example.vpnserverdatagram.ip;

public class Checksum {

    public static short calculate(byte[] data, int offset, int length) {
        long sum = 0;
        int i = offset;

        while (length > 1) {
            sum += ((data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF);
            i += 2;
            length -= 2;
        }

        if (length > 0) {
            sum += (data[i] & 0xFF) << 8;
        }

        while ((sum >> 16) != 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }

        return (short) ~sum;
    }
}
