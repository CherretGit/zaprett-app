package com.cherret.zaprett.byedpi

class NativeBridge {
    companion object {
        init {
            System.loadLibrary("byedpi")
        }
    }

    external fun jniStartProxy(args: Array<String>): Int
    external fun jniStopProxy(): Int
}