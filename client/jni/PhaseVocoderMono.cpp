/*
 * PhaseVocoderMono.cpp
 *
 *  Created on: Oct 22, 2015
 *      Author: scpark
 */

#include "PhaseVocoderMono.h"


PhaseVocoderMono::PhaseVocoderMono(int sampleRate, int FFTLogSize, int bufferSampleLength, int overlapRatio)
:Plugin(sampleRate, bufferSampleLength), FFTLogSize(FFTLogSize), overlapRatio(overlapRatio)
{
	// TODO Auto-generated constructor stub
	if(FFTLogSize == 1) FFTSize = 2;
	else if(FFTLogSize == 2) FFTSize = 4;
	else if(FFTLogSize == 3) FFTSize = 8;
	else if(FFTLogSize == 4) FFTSize = 16;
	else if(FFTLogSize == 5) FFTSize = 32;
	else if(FFTLogSize == 6) FFTSize = 64;
	else if(FFTLogSize == 7) FFTSize = 128;
	else if(FFTLogSize == 8) FFTSize = 256;
	else if(FFTLogSize == 9) FFTSize = 512;
	else if(FFTLogSize == 10) FFTSize = 1024;
	else if(FFTLogSize == 11) FFTSize = 2048;
	else if(FFTLogSize == 12) FFTSize = 4096;
	else if(FFTLogSize == 13) FFTSize = 8192;
	else if(FFTLogSize == 14) FFTSize = 16384;
	else if(FFTLogSize == 15) FFTSize = 32768;
	else if(FFTLogSize == 16) FFTSize = 65536;

	hopSize = FFTSize / overlapRatio;
	allocateMonoBuffer(currentRealBuffer, FFTSize);
	allocateMonoBuffer(currentImagBuffer, FFTSize);

	allocateMonoBuffer(currentPhaseBuffer, FFTSize);

	allocateMonoBuffer(previousRealBuffer, FFTSize);
	allocateMonoBuffer(previousImagBuffer, FFTSize);

	allocateMonoBuffer(phaseAcc, FFTSize);
	allocateMonoBuffer(prevPhase, FFTSize);

	for(int i=0;i<FFTSize;i++)
	{
		phaseAcc[i] = 0.0f;
		prevPhase[i] = 0.0f;

	}

	windowBuffer = (float*) malloc(FFTSize * sizeof(float));
	AudioTool::putWindowBuffer(windowBuffer, FFTSize);

	inputBuffer = new RingBuffer<float>(FFTSize * 128);
	outputBuffer = new RingBuffer<float>(FFTSize * 128);

	pthread_mutex_init(&mutex, NULL);
}

PhaseVocoderMono::~PhaseVocoderMono() {
	// TODO Auto-generated destructor stub

	releaseMonoBuffer(currentRealBuffer);
	releaseMonoBuffer(currentImagBuffer);

	releaseMonoBuffer(previousRealBuffer);
	releaseMonoBuffer(previousImagBuffer);

	releaseMonoBuffer(phaseAcc);

	free(windowBuffer);
}

int PhaseVocoderMono::put(float *inBuffer, int sampleLength)
{
	pthread_mutex_lock(&mutex);
	inputBuffer->writeMono(inBuffer, sampleLength, sampleLength, false);
	pthread_mutex_unlock(&mutex);
	return sampleLength;
}

int PhaseVocoderMono::get(float *outBuffer, int sampleLength)
{
	pthread_mutex_lock(&mutex);
	double currentPlayRate = this->playRate;
	double currentPitchRate = this->pitchRate;
	double positionIncrement = currentPlayRate * hopSize;

	while(inputBuffer->getRemainedSampleLength() > ceil(FFTSize * currentPitchRate) && outputBuffer->getRemainedSampleLength()<sampleLength)
	{
		inputBuffer->readMono(currentRealBuffer, 0.0, FFTSize, currentPitchRate, 0);
		memset(currentImagBuffer, 0, sizeof(float) * FFTSize);

		inputBuffer->readMono(previousRealBuffer, -(hopSize * currentPitchRate), FFTSize, currentPitchRate, positionIncrement);
		memset(previousImagBuffer, 0, sizeof(float) * FFTSize);

		doManipulation();
		outputBuffer->writeMono(currentRealBuffer, FFTSize, hopSize, true);
	}
	int readSamples = outputBuffer->readMono(outBuffer, 0, sampleLength, 1.0f, sampleLength);
	pthread_mutex_unlock(&mutex);
	return readSamples;
}

int PhaseVocoderMono::getRemainedBufferSamples()
{
	return inputBuffer->getRemainedSampleLength();
}

int PhaseVocoderMono::process(float *inBuffer, float *outBuffer, int sampleLength, channel_t channel)
{
	inputBuffer->writeMono(inBuffer, sampleLength, sampleLength, false);
	float currentPlayRate = this->playRate;
	float currentPitchRate = this->pitchRate;
	double positionIncrement = currentPlayRate * hopSize;

	while(inputBuffer->getRemainedSampleLength() > ceil(FFTSize * currentPitchRate))
	{
		inputBuffer->readMono(currentRealBuffer, 0.0, FFTSize, currentPitchRate, 0);
		memset(currentImagBuffer, 0, sizeof(float) * FFTSize);

		inputBuffer->readMono(previousRealBuffer, -(hopSize * currentPitchRate), FFTSize, currentPitchRate, positionIncrement);
		memset(previousImagBuffer, 0, sizeof(float) * FFTSize);

		doManipulation();
		outputBuffer->writeMono(currentRealBuffer, FFTSize, hopSize, true);
	}

	int readSamples = outputBuffer->readAsManyAsPossibleMono(outBuffer, 0, 1.0f);
	return readSamples;
}

int PhaseVocoderMono::process(float *inBuffer, float *outBuffer, int sampleLength, int wantSampleLength, channel_t channel)
{
	pthread_mutex_lock(&mutex);
		inputBuffer->writeMono(inBuffer, sampleLength, sampleLength, false);
		double currentPlayRate = this->playRate;
		double currentPitchRate = this->pitchRate;
		double positionIncrement = currentPlayRate * hopSize;

		while(inputBuffer->getRemainedSampleLength() > ceil(FFTSize * currentPitchRate) && outputBuffer->getRemainedSampleLength()<wantSampleLength)
		{
			inputBuffer->readMono(currentRealBuffer, 0.0, FFTSize, currentPitchRate, 0);
			memset(currentImagBuffer, 0, sizeof(float) * FFTSize);

			inputBuffer->readMono(previousRealBuffer, -(hopSize * currentPitchRate), FFTSize, currentPitchRate, positionIncrement);
			memset(previousImagBuffer, 0, sizeof(float) * FFTSize);

			doManipulation();
			outputBuffer->writeMono(currentRealBuffer, FFTSize, hopSize, true);
		}
		int readSamples = outputBuffer->readMono(outBuffer, 0, wantSampleLength, 1.0f, wantSampleLength);
	pthread_mutex_unlock(&mutex);
	return readSamples;

}

int PhaseVocoderMono::doManipulation()
{
	// FFT
	AudioTool::bufferMultiply(currentRealBuffer, currentRealBuffer, windowBuffer, FFTSize);
	SuperpoweredPolarFFT(currentRealBuffer, currentImagBuffer, FFTLogSize+1, true);

	AudioTool::bufferMultiply(previousRealBuffer, previousRealBuffer, windowBuffer, FFTSize);
	SuperpoweredPolarFFT(previousRealBuffer, previousImagBuffer, FFTLogSize+1, true);

	for(int i=0;i<FFTSize/2;i++)
	{
		float currentPhase = currentImagBuffer[i];
		float previousPhase = previousImagBuffer[i];
		phaseAcc[i] += currentPhase - previousPhase;
		currentImagBuffer[i] = AudioTool::wrapPhase(phaseAcc[i]);
		if(i>0)
		{
			currentRealBuffer[FFTSize-i] = currentRealBuffer[i];
			currentImagBuffer[FFTSize-i] = -currentImagBuffer[i];
		}
	}

	// IFFT
	SuperpoweredPolarFFT(currentRealBuffer, currentImagBuffer, FFTLogSize+1, false);
	AudioTool::bufferMultiply(currentRealBuffer, currentRealBuffer, windowBuffer, FFTSize);

	for(int i=0;i<FFTSize;i++)
		currentRealBuffer[i] /= (FFTSize+1) * overlapRatio / 2;
	return 0;
}


int PhaseVocoderMono::reset()
{
	inputBuffer->reset();
	outputBuffer->reset();

	for(int i=0;i<FFTSize;i++)
	{
		phaseAcc[i] = 0.0f;
		prevPhase[i] = 0.0f;
	}
}

int PhaseVocoderMono::setPitchRate(double pitchRate)
{
	this->pitchRate = pitchRate;
}

int PhaseVocoderMono::setPlayRate(double playRate)
{
	if(playRate > 2.0d)
		this->playRate = this->basePlayRate * 2.0d;
	else
		this->playRate = this->basePlayRate * playRate;
}

int PhaseVocoderMono::setBasePlayRate(double basePlayRate)
{
	this->basePlayRate = basePlayRate;
	this->playRate = basePlayRate;
}
