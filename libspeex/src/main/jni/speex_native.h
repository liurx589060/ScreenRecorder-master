/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
#include <android/log.h>
/* Header for class com_example_libspeex_SpeexNative */

#ifndef _Included_com_example_libspeex_SpeexNative
#define _Included_com_example_libspeex_SpeexNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeInitEcho
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeInitEcho
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeProcEcho
 * Signature: ([B[B[B)I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeProcEcho
  (JNIEnv *, jclass, jbyteArray, jbyteArray, jbyteArray);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeCloseEcho
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeCloseEcho
  (JNIEnv *, jclass);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeInitDeNose
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeInitDeNose
  (JNIEnv *, jclass, jint, jint, jint);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeProcDeNose8K
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeProcDeNose8K
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeProcDeNose16K
 * Signature: ([B)I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeProcDeNose16K
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     com_example_libspeex_SpeexNative
 * Method:    nativeCloseDeNose
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_example_libspeex_SpeexNative_nativeCloseDeNose
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
