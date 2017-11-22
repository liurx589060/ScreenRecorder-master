package com.example.libspeex;

/**
 * Created by Administrator on 2017/10/21.
 */

public class SpeexNative {
    static {
        System.loadLibrary("speex");
    }

    //消除回音
    public static native int nativeInitEcho(int frame_size,int filter_length,int sampling_rate);
    public static native int nativeProcEcho(byte[] recordArray,byte[] playArray,byte[] szOutArray);
    public static native int nativeCloseEcho();

    //消除噪声
    public static native int nativeInitDeNose(int frame_size,int filter_length,int sampling_rate);
    public static native int nativeProcDeNose8K(byte[] recordArray);
    public static native int nativeProcDeNose16K(byte[] recordArray);
    public static native int nativeCloseDeNose();
}
