package org.apache.metron.test.converters;

/**
 * Created by cstella on 1/27/16.
 */
public class HexStringConverter implements IConverter {
    public byte[] convert(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }
}
