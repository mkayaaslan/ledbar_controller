package com.example.elcapi;

/**
 * JNI bridge to the native libjnielc.so library.
 *
 * Provides low-level access to the LED bar controller via /dev/ledjni on
 * iiyama B1PNR and B3PNR 16" tablets. Uses ioctl to set individual RGB channels.
 *
 * Channel flags:
 *   0xA1 = Right Red,  0xA2 = Right Green,  0xA3 = Right Blue
 *   0xB1 = Left Red (mono)
 *
 * Not available on B3PNR 10" (seekstart returns -1).
 */
public class jnielc {
    public static native int ledoff();
    public static native int seekstart();
    public static native int seekstop();
    public static native int ledseek(int flag, int progress);
    static { System.loadLibrary("jnielc"); }
}