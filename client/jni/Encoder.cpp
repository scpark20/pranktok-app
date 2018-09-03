/*
 * Encoder.cpp
 *
 *  Created on: Mar 8, 2016
 *      Author: scpark
 */

#include "Encoder.h"

Encoder::Encoder() {
	// TODO Auto-generated constructor stub

}

Encoder::~Encoder() {
	// TODO Auto-generated destructor stub
}

int Encoder::init(int sampleRate, int bitRate)
{
	if(handle!=NULL)
		if(aacEncOpen(&handle, 0, 1) != AACENC_OK)
			return -1;

	if(aacEncOpen(&handle, 0, 1) != AACENC_OK)
		return -1;

	if(aacEncoder_SetParam(handle, AACENC_AOT, 2) != AACENC_OK)
		return -2;

	if(aacEncoder_SetParam(handle, AACENC_SBR_MODE, 1) != AACENC_OK)
		return -3;

	if(aacEncoder_SetParam(handle, AACENC_SAMPLERATE, sampleRate) != AACENC_OK)
		return -4;

	if(aacEncoder_SetParam(handle, AACENC_CHANNELMODE, MODE_1) != AACENC_OK)
		return -5;

	if(aacEncoder_SetParam(handle, AACENC_CHANNELORDER, 1) != AACENC_OK)
		return -6;

	if(aacEncoder_SetParam(handle, AACENC_BITRATEMODE, 0) != AACENC_OK)
		return -7;

	if(aacEncoder_SetParam(handle, AACENC_BITRATE, 32000) != AACENC_OK)
		return -7;

	if(aacEncoder_SetParam(handle, AACENC_TRANSMUX, 2) != AACENC_OK)
		return -8;

	if(aacEncoder_SetParam(handle, AACENC_AFTERBURNER, 0) != AACENC_OK)
		return -9;

	if(aacEncEncode(handle, NULL, NULL, NULL, NULL) != AACENC_OK)
		return -10;

	if(aacEncInfo(handle, &info) != AACENC_OK)
		return -11;

	return 0;
}

int Encoder::encode(uint8_t* inputBuffer, int inputLength, uint8_t* outputBuffer, int outputLength)
{
	in_buf = { 0 };
	out_buf =  { 0 };
	in_args = { 0 };
	out_args = { 0 };
	int in_identifier = IN_AUDIO_DATA;
	int out_identifier = OUT_BITSTREAM_DATA;
	AACENC_ERROR err;

	in_buf.numBufs = 1;
	in_buf.bufs = (void**)&inputBuffer;
	in_buf.bufferIdentifiers = &in_identifier;
	in_buf.bufSizes = &inputLength;
	int elemSize = 2;
	in_buf.bufElSizes = &elemSize;

	in_args.numInSamples = inputLength / 2;

	out_buf.numBufs = 1;
	out_buf.bufs = (void**)&outputBuffer;
	out_buf.bufferIdentifiers = &out_identifier;
	out_buf.bufSizes = &outputLength;
	int outElemSize = 1;
	out_buf.bufElSizes = &outElemSize;

	if((err = aacEncEncode(handle, &in_buf, &out_buf, &in_args, &out_args)) != AACENC_OK)
		return -1;

	return out_args.numOutBytes;
}
