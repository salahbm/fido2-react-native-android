package com.example.testlib;

public class MainActivity {
    public MainActivity() {

    }

    public static boolean LoadLib() {
        try {
            System.loadLibrary("testlib");
            return true;
        } catch (Exception ex) {
            System.err.println("WARNING: Could not load library");
            return false;
        }
    }

    public native String stringFromJNI();
}
