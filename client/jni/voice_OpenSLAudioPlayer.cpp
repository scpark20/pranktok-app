#include "voice_OpenSLAudioPlayer.h"
JNIEXPORT void JNICALL Java_voice_OpenSLAudioPlayer_native_1init
(JNIEnv *env, jobject clazz, jint systemSampleRate, jint sampleRate, jint bufferSize)
{
	AudioPlayer::getInstance()->init(systemSampleRate, sampleRate, bufferSize);

}

JNIEXPORT void JNICALL Java_voice_OpenSLAudioPlayer_native_1uninit
  (JNIEnv *env, jobject clazz)
{
	AudioPlayer::getInstance()->uninit();
}

JNIEXPORT void JNICALL Java_voice_OpenSLAudioPlayer_native_1start
  (JNIEnv *env, jobject clazz)
{
	AudioPlayer::getInstance()->start();
}

JNIEXPORT void JNICALL Java_voice_OpenSLAudioPlayer_native_1stop
	(JNIEnv *env, jobject clazz)
{
	AudioPlayer::getInstance()->stop();
}

JNIEXPORT void JNICALL Java_voice_OpenSLAudioPlayer_native_1put
  (JNIEnv *env, jobject clazz, int seqNumber, jbyteArray byteArray)
{
	int length = env->GetArrayLength(byteArray);
	jbyte *array = env->GetByteArrayElements(byteArray, 0);

	AudioPlayer::getInstance()->put(seqNumber, (uint8_t*)array, length);
	env->ReleaseByteArrayElements(byteArray, array, 0);
}
