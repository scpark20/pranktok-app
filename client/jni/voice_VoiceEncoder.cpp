#include "voice_VoiceEncoder.h"

static Encoder* encoder = NULL;

/*
 * Class:     voice_VoiceEncoder
 * Method:    native_init
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_voice_VoiceEncoder_native_1init
  (JNIEnv *env, jobject clazz, jint sampleRate, jint bitRate)
{
	if(encoder!=NULL)
		delete encoder;

	encoder = new Encoder();
	encoder->init(sampleRate, bitRate);
}

/*
 * Class:     voice_VoiceEncoder
 * Method:    native_put
 * Signature: ([B[B)I
 */
JNIEXPORT jint JNICALL Java_voice_VoiceEncoder_native_1put
  (JNIEnv *env, jobject clazz, jshortArray inputArray, jbyteArray outputArray)
{
	int inputLength = env->GetArrayLength(inputArray);
	int outputLength = env->GetArrayLength(outputArray);
	jshort *inputShortArray = env->GetShortArrayElements(inputArray, 0);
	jbyte *outputByteArray = env->GetByteArrayElements(outputArray, 0);


	int n = encoder->encode((uint8_t*) inputShortArray, inputLength*2, (uint8_t*) outputByteArray, outputLength);
	env->ReleaseShortArrayElements(inputArray, inputShortArray, 0);
	env->ReleaseByteArrayElements(outputArray, outputByteArray, 0);
	return n;
}

/*
 * Class:     voice_VoiceEncoder
 * Method:    native_reset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_voice_VoiceEncoder_native_1reset
  (JNIEnv *env, jobject clazz)
{
	/*
	if(encoder!=NULL)
		encoder->reset();
		*/
}
