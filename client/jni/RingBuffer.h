/*
 * RingBuffer.h
 *
 *  Created on: Oct 23, 2015
 *      Author: scpark
 */

#ifndef RINGBUFFER_H_
#define RINGBUFFER_H_

#include "AudioTool.h"
#include "LogHelper.h"
#include "pthread.h"
#include <stdlib.h>

template <typename T>
class RingBuffer {
public:
	RingBuffer(int bufferSampleLength)
	{
		this->bufferSampleLength = bufferSampleLength;
		buffer = (T*) malloc(bufferSampleLength * sizeof(T));

		pthread_mutex_init(&mutex, NULL);
		writePosition = 0;
		writeUpperPosition = 0;
		readPosition = 0;
	}

	virtual ~RingBuffer()
	{
		free(buffer);
	}

	int writeMono(T *sourceBuffer, int sampleLength, int positionIncrement, bool MixWrite)
		{

			pthread_mutex_lock(&mutex);
			int modedWritePosition = writePosition % bufferSampleLength;
			int copySamples = modedWritePosition + sampleLength > bufferSampleLength ?
								bufferSampleLength - modedWritePosition :
								sampleLength;

			if(MixWrite)
			{
				int zeroLowerBufferPosition = writeUpperPosition % bufferSampleLength;
				int zeroUpperBufferPosition = (writePosition + sampleLength) % bufferSampleLength;

				if(zeroUpperBufferPosition < zeroLowerBufferPosition)
				{
					memset(&buffer[zeroLowerBufferPosition], 0, (bufferSampleLength - zeroLowerBufferPosition) * sizeof(float));
					memset(&buffer[0], 0, zeroUpperBufferPosition * sizeof(float));
				}
				else
					memset(&buffer[zeroLowerBufferPosition], 0, (zeroUpperBufferPosition - zeroLowerBufferPosition) * sizeof(float));


				for(int i=0;i<copySamples;i++)
				{
					buffer[(modedWritePosition+i)] += sourceBuffer[i];
				}

				for(int i=0;i<sampleLength-copySamples;i++)
				{
					buffer[i] += sourceBuffer[(copySamples+i)];
				}

			}
			else
			{
				memcpy(&buffer[modedWritePosition], sourceBuffer, copySamples * sizeof(T));
				memcpy(buffer, &sourceBuffer[copySamples], (sampleLength-copySamples) * sizeof(T));
			}

			writeUpperPosition = writePosition + sampleLength;
			writePosition += positionIncrement;
			//LOGI("%s %d %d %d %d", __FUNCTION__, sampleLength, writePosition, writeUpperPosition, positionIncrement);
			pthread_mutex_unlock(&mutex);
			return writePosition;
		}




	int readAsManyAsPossibleMono(T *destBuffer, int readOffset, float readInterval)
	{


		pthread_mutex_lock(&mutex);
		double readSamples = 0;
		int i = 0;
		while(readPosition + readSamples < writePosition)
		{
			double position = AudioTool::getRingBufferFloatPosition(readPosition + readOffset + readSamples, bufferSampleLength);
			AudioTool::getMonoSampleByFloatIndex2(buffer, bufferSampleLength, position, &destBuffer[i]);
			readSamples += readInterval;
			i++;
		}

		readPosition += readSamples;
		pthread_mutex_unlock(&mutex);
		return i;
	}

	int readMono(T *destBuffer, double readOffset, int sampleLength, double readInterval, double positionIncrement)
	{
		pthread_mutex_lock(&mutex);
		int i=0;
		for(i=0;i<sampleLength;i++)
		{

			if(readPosition + readOffset + i * readInterval > writePosition)
				break;

			double position = AudioTool::getRingBufferFloatPosition(readPosition + readOffset + i * readInterval, bufferSampleLength);
			AudioTool::getMonoSampleByFloatIndex2(buffer, bufferSampleLength, position, &destBuffer[i]);
		}

		readPosition += (positionIncrement * (double)i / (double) sampleLength);

		//LOGI("%s %f %d", __FUNCTION__, readPosition, writePosition);
		pthread_mutex_unlock(&mutex);
		return i;
	}


	int getRemainedSampleLength()
	{
		int ret;
		pthread_mutex_lock(&mutex);
		ret = writePosition - (int) readPosition;
		pthread_mutex_unlock(&mutex);
		return ret;

	}

	int reset()
	{
		this->writePosition = 0;
		this->readPosition = 0;

		memset(buffer, 0, bufferSampleLength * sizeof(T));
	}

private:
	T* buffer;
	int bufferSampleLength;
	pthread_mutex_t mutex;
	int writePosition;
	int writeUpperPosition;
	double readPosition;
};

#endif /* RINGBUFFER_H_ */
