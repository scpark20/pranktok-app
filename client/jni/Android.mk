 LOCAL_PATH := $(call my-dir)
 
  include $(CLEAR_VARS)
LOCAL_MODULE:= libfraunhoferaac
LOCAL_SRC_FILES:= FraunhoferAAC/libFraunhoferAAC.a
include $(PREBUILT_STATIC_LIBRARY)
 
 include $(CLEAR_VARS)
 LOCAL_MODULE    := prankcallclient
 LOCAL_C_INCLUDES := $(LOCAL_PATH)/include \
  					$(LOCAL_PATH)/libAACdec/include \
 					$(LOCAL_PATH)/libAACenc/include \
 					$(LOCAL_PATH)/libFDK/include \
 					$(LOCAL_PATH)/libMpegTPDec/include \
 					$(LOCAL_PATH)/libMpegTPEnc/include \
 					$(LOCAL_PATH)/libPCMutils/include \
 					$(LOCAL_PATH)/libSBRdec/include \
 					$(LOCAL_PATH)/libSBRenc/include \
 					$(LOCAL_PATH)/libSYS/include
 LOCAL_SRC_FILES := voice_OpenSLAudioPlayer.cpp \
 					AudioPlayer.cpp \
 					AudioTool.cpp \
 					PhaseVocoderMono.cpp \
 					voice_PhaseVocoder.cpp \
 					Encoder.cpp \
 					Decoder.cpp \
 					voice_VoiceEncoder.cpp \
 					PriorityQueue.cpp
 LOCAL_STATIC_LIBRARIES := cpufeatures \
 						   Superpowered \
						   libfraunhoferaac
 LOCAL_ARM_NEON := true
 LOCAL_LDLIBS += -lOpenSLES -llog
 
 LOCAL_CFLAGS = -O3 -D__STDC_CONSTANT_MACROS

 include $(BUILD_SHARED_LIBRARY)
 
SUPERPOWERED_PATH := Superpowered

include $(CLEAR_VARS)
LOCAL_MODULE := Superpowered
LOCAL_SRC_FILES := $(SUPERPOWERED_PATH)/libSuperpoweredAndroidARM.a
include $(PREBUILT_STATIC_LIBRARY)

 
 $(call import-module,android/cpufeatures)