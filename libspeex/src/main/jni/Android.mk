LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE:= libspeex
LOCAL_CFLAGS = -DFIXED_POINT -DUSE_KISS_FFT -DEXPORT="" -UHAVE_CONFIG_H
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_C_INCLUDES += speex_native.h

LOCAL_SRC_FILES := $(wildcard $(LOCAL_PATH)/libspeexdsp/*.c)

#LOCAL_SRC_FILES :=  libspeex/cb_search.c  libspeex/exc_10_32_table.c 	libspeex/exc_8_128_table.c \
                  	libspeex/filters.c 	libspeex/gain_table.c 	libspeex/hexc_table.c 	libspeex/high_lsp_tables.c 	libspeex/lsp.c \
                  	libspeex/ltp.c 	libspeex/speex.c 	libspeex/stereo.c 	libspeex/vbr.c 	libspeex/vq.c libspeex/bits.c libspeex/exc_10_16_table.c \
                  	libspeex/exc_20_32_table.c libspeex/exc_5_256_table.c libspeex/exc_5_64_table.c libspeex/gain_table_lbr.c libspeex/hexc_10_32_table.c \
                  	libspeex/lpc.c libspeex/lsp_tables_nb.c libspeex/modes.c libspeex/modes_wb.c libspeex/nb_celp.c libspeex/quant_lsp.c libspeex/sb_celp.c \
                  	libspeex/speex_callbacks.c libspeex/speex_header.c libspeex/window.c\

 LOCAL_SRC_FILES += speex_native.c

 LOCAL_LDLIBS :=-llog


#创建static静态库，动态库为BUILD_SHARED_LIBRARY
include $(BUILD_SHARED_LIBRARY)