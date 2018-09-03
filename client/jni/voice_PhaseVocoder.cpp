
#include "voice_PhaseVocoder.h"

static PhaseVocoderMono *phaseVocoderMono = NULL;
static float* floatBuffer = NULL;

/*
 * Class:     voice_PhaseVocoder
 * Method:    native_init
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_voice_PhaseVocoder_native_1init
  (JNIEnv *env, jobject clazz, jint sampleRate, jint sampleLength, jint FFTLogSize, jint overlapRatio)
{
	if(phaseVocoderMono!=NULL)
		delete phaseVocoderMono;

	if(floatBuffer!=NULL)
		free(floatBuffer);

	phaseVocoderMono = new PhaseVocoderMono(sampleRate, FFTLogSize, sampleLength, overlapRatio);

	floatBuffer = (float*) malloc(sizeof(float) * sampleLength * 128);
}

/*
 * Class:     voice_PhaseVocoder
 * Method:    native_put
 * Signature: ([B[B)I
 */
JNIEXPORT jint JNICALL Java_voice_PhaseVocoder_native_1put___3S_3S
  (JNIEnv *env, jobject clazz, jshortArray inputArray, jshortArray outputArray)
{
	int inputLength = env->GetArrayLength(inputArray);
	int outputLength = env->GetArrayLength(outputArray);
	jshort *inputShortArray = env->GetShortArrayElements(inputArray, 0);
	jshort *outputShortArray = env->GetShortArrayElements(outputArray, 0);

	AudioTool::short2float((short*)inputShortArray, floatBuffer, inputLength);

	int n = phaseVocoderMono->process(floatBuffer, floatBuffer, inputLength, outputLength);
	AudioTool::float2short(floatBuffer, (short*) outputShortArray, n);

	env->ReleaseShortArrayElements(inputArray, inputShortArray, 0);
	env->ReleaseShortArrayElements(outputArray, outputShortArray, 0);

	return n;
}

JNIEXPORT jint JNICALL Java_voice_PhaseVocoder_native_1put___3S
  (JNIEnv *env, jobject clazz, jshortArray inputArray)
{
	int inputLength = env->GetArrayLength(inputArray);
	jshort *inputShortArray = env->GetShortArrayElements(inputArray, 0);

	AudioTool::short2float((short*)inputShortArray, floatBuffer, inputLength);
	int n = phaseVocoderMono->put(floatBuffer, inputLength);

	env->ReleaseShortArrayElements(inputArray, inputShortArray, 0);
	return 0;
}

JNIEXPORT jint JNICALL Java_voice_PhaseVocoder_native_1get
  (JNIEnv *env, jobject clazz, jshortArray outputArray)
{
	int outputLength = env->GetArrayLength(outputArray);
	jshort *outputShortArray = env->GetShortArrayElements(outputArray, 0);

	int n = phaseVocoderMono->get(floatBuffer, outputLength);
	AudioTool::float2short(floatBuffer, (short*) outputShortArray, n);

	env->ReleaseShortArrayElements(outputArray, outputShortArray, 0);
	return n;
}

/*
 * Class:     voice_PhaseVocoder
 * Method:    native_reset
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_voice_PhaseVocoder_native_1reset
  (JNIEnv *env, jobject clazz)
{
	phaseVocoderMono->reset();
}

JNIEXPORT void JNICALL Java_voice_PhaseVocoder_native_1setPitchRate
  (JNIEnv *env, jobject clazz, jdouble pitchRate)
{
	phaseVocoderMono->setPitchRate(pitchRate);
}

