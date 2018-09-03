/*
 * AudioPlayer.h
 *
 *  Created on: Feb 19, 2016
 *      Author: scpark
 */

#include <stdio.h>
#include <stdlib.h>
#include <jni.h>
#include "LogHelper.h"

#include <sched.h>
#include <unistd.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SLES/OpenSLES_AndroidMetadata.h>
#include <SLES/OpenSLES_Platform.h>
#include <SLES/OpenSLES_AndroidConfiguration.h>
#include <sched.h>
#include "PhaseVocoderMono.h"
#include "PriorityQueue.h"
#include "Decoder.h"

#ifndef AUDIOPLAYER_H_
#define AUDIOPLAYER_H_

typedef struct CallbackCntxt_ {
	uint8_t* byteBuffer1;
	uint8_t* byteBuffer2;
	float* floatBuffer;
	PhaseVocoderMono* phaseVocoderMono;
    PriorityQueue *priorityQueue;
    Decoder *decoder;


} CallbackContext;

class AudioPlayer {
public:
	virtual ~AudioPlayer();
	static AudioPlayer* getInstance();
	void init(int systemSampleRate, int sampleRate, int bufferSize);
	void uninit();
	void start();
	void stop();
	void put(int seqNumber, uint8_t* buffer, int length);

	static void bufferQueueCallback(SLBufferQueueItf queueItf, void *context);

private:
	AudioPlayer();

	SLObjectItf slEngine;
	SLObjectItf player;
	SLObjectItf outputMix;

	SLEngineItf engineItf;
	SLPlayItf playItf;
	SLBufferQueueItf bufferQueueItf;
	SLVolumeItf volumeItf;
	SLAndroidConfigurationItf playerConfig;

	SLBufferQueueState state;
	CallbackContext *context;

	float* floatBuffer;
	float basePitchRate;
	float basePlayRate;

};

static AudioPlayer* instance = NULL;

#endif /* AUDIOPLAYER_H_ */
