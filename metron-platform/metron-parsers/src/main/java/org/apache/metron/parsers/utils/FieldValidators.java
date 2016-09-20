package org.apache.metron.parsers.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldValidators {

    public static boolean isValidPort(int portNumber) {
        if (portNumber > 1 && portNumber < 65536)
            return true;
        else
            return false;
    }

    public static boolean isValidIpAddr(String ipAddress) {
        String pattern = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(ipAddress);
        if (m.matches())
            return true;
        else
            return false;
    }
}
