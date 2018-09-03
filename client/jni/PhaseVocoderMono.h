/*
 * PhaseVocoderMono.h
 *
 *  Created on: Oct 22, 2015
 *      Author: scpark
 */

#ifndef PHASEVOCODERMONO_H_
#define PHASEVOCODERMONO_H_

#include "Plugin.h"
#include <math.h>
#include <stdlib.h>
#include <SuperpoweredFFT.h>
#include "RingBuffer.h"
#include <AudioTool.h>
#include "LogHelper.h"
#include <pthread.h>

class PhaseVocoderMono: public Plugin {
public:
	PhaseVocoderMono(int sampleRate, int FFTLogSize, int bufferSampleLength, int overlapRatio);
	virtual ~PhaseVocoderMono();

	int put(float *inBuffer, int sampleLength);
	int get(float *outBuffer, int sampleLength);
	virtual int process(float *inBuffer, float *outBuffer, int sampleLength, channel_t channel = BOTH);
	int process(float *inBuffer, float *outBuffer, int sampleLength, int wantSampleLength, channel_t channel = BOTH);
	virtual int reset();

	int setPitchRate(double pitchRate);
	int setPlayRate(double playRate);
	double getPlayRate(){return this->playRate;}
	int setBasePlayRate(double basePlayRate);
	int getRemainedBufferSamples();

private:
	int doManipulation();
	// environment variables
	int sampleRate;
	int FFTLogSize;
	int FFTSize;
	int hopSize;
	int bufferSampleLength;
	int overlapRatio;

	// own buffer
	RingBuffer<float> *inputBuffer;
	RingBuffer<float> *outputBuffer;

	float *currentRealBuffer;
	float *currentImagBuffer;

	float *currentPhaseBuffer;

	float *previousRealBuffer;
	float *previousImagBuffer;

	float *windowBuffer;

	//phase related variables
	float *phaseAcc;	//Accumulation
	float *prevPhase; //previous Phase


	double pitchRate = 1.0d;
	double basePlayRate = 1.0d;
	double playRate = 1.0d;

	pthread_mutex_t mutex;
};

#endif /* PHASEVOCODERMONO_H_ */
