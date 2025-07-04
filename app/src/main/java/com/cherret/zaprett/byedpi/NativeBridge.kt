package com.cherret.zaprett.byedpi

class NativeBridge {
    companion object {
        init {
            System.loadLibrary("byedpi")
        }
    }
    fun startProxy(args: Array<String>): Int {
        jniCreateSocket(args)
        return jniStartProxy()
    }

    fun stopProxy(): Int {
        return jniStopProxy()
    }

    private external fun jniCreateSocket(args: Array<String>): Int
    private external fun jniStartProxy(): Int
    private external fun jniStopProxy(): Int
}