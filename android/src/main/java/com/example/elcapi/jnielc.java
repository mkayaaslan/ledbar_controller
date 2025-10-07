package com.example.elcapi;

public class jnielc {
    public static native int ledoff();
    public static native int seekstart();
    public static native int seekstop();
    public static native int ledseek(int flag, int progress);
    static { System.loadLibrary("jnielc"); }
}