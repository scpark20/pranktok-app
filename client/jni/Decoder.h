/*
 * Decoder.h
 *
 *  Created on: Mar 8, 2016
 *      Author: scpark
 */

#ifndef DECODER_H_
#define DECODER_H_
#include <aacdecoder_lib.h>
#include <stdlib.h>
#include <stdint.h>
#include "LogHelper.h"

class Decoder {
public:
	Decoder();
	virtual ~Decoder();

	int init();
	int decode(uint8_t* inputBuffer, int inputLength, uint8_t* outputBuffer, int outputLength);
	int reset();
private:
	HANDLE_AACDECODER handle = NULL;

};

#endif /* DECODER_H_ */
