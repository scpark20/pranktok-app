/*
 * Decoder.h
 *
 *  Created on: Mar 5, 2016
 *      Author: scpark
 */

#ifndef DECODER_H_
#define DECODER_H_
extern "C" {
#include <libavcodec/avcodec.h>
}
#include "LogHelper.h"
#include "AudioTool.h"
#include <stdlib.h>

class Decoder {
public:
	Decoder();
	virtual ~Decoder();
	bool init(int sampleRate);
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

#endif /* DECODER_H_ */
