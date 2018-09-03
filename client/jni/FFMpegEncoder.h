/*
 * Encoder.h
 *
 *  Created on: Mar 5, 2016
 *      Author: scpark
 */

#ifndef ENCODER_H_
#define ENCODER_H_
extern "C" {
#include <libavcodec/avcodec.h>
}
#include "LogHelper.h"
#include "AudioTool.h"
#include <stdlib.h>

class Encoder {
public:
	Encoder();
	virtual ~Encoder();
	bool init(int sampleRate, int bitRate);
	int put(uint8_t* inputBuffer, int inputLength, uint8_t* outBuffer, int outputLength);
	void reset();

private:
	int SAMPLE_RATE;
	int BIT_RATE;

	AVCodecContext *codecContext;
	AVCodec *codec;

	AVPacket avPacket;
	AVFrame *avFrame;


};

#endif /* ENCODER_H_ */
