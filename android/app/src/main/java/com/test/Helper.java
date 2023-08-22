package com.test;

import com.example.testlib.MainActivity;

public class Helper {
    private static MainActivity jniLoader;
    private static boolean libLoaded = false;

    public static void init() {
        if (!libLoaded) {
            libLoaded = MainActivity.LoadLib();
        }
        if (libLoaded) {
            jniLoader = new MainActivity();
        }
    }

    public static String stringFromJNI() {
        return jniLoader.stringFromJNI();
    }
}
