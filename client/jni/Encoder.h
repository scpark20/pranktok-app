/*
 * Encoder.h
 *
 *  Created on: Mar 8, 2016
 *      Author: scpark
 */

#ifndef ENCODER_H_
#define ENCODER_H_
#include <aacenc_lib.h>
#include <stdlib.h>
#include <stdint.h>
#include "LogHelper.h"

class Encoder {
public:
	Encoder();
	virtual ~Encoder();

	int init(int sampleRate, int bitRate);
	int encode(uint8_t* inputBuffer, int inputLength, uint8_t* outputBuffer, int outputLength);

private:
	HANDLE_AACENCODER handle = NULL;
	AACENC_BufDesc in_buf;
	AACENC_BufDesc out_buf;
	AACENC_InArgs in_args;
	AACENC_OutArgs out_args;
	AACENC_InfoStruct info = { 0 };
};

#endif /* ENCODER_H_ */
