/*
 * Decoder.cpp
 *
 *  Created on: Mar 8, 2016
 *      Author: scpark
 */

#include "Decoder.h"

Decoder::Decoder() {
	// TODO Auto-generated constructor stub

}

Decoder::~Decoder() {
	// TODO Auto-generated destructor stub
}

int Decoder::init()
{
	if(handle!=NULL)
		aacDecoder_Close(handle);

	handle = aacDecoder_Open(TT_MP4_ADTS, 1);
	if(!handle)
		return -1;


	if (aacDecoder_SetParam(handle, AAC_CONCEAL_METHOD, 2) != AAC_DEC_OK)
	     return -2;
/*
	if (aacDecoder_SetParam(handle, AAC_PCM_MAX_OUTPUT_CHANNELS, 0) != AAC_DEC_OK)
		 return -3;
*/
	return 0;
}

int Decoder::decode(uint8_t* inputBuffer, int inputLength, uint8_t* outputBuffer, int outputLength)
{
    AAC_DECODER_ERROR err;
    UINT valid = inputLength;

    UCHAR* inBuffer[1];
    UINT inBufferLength[1] = {0};
    inBuffer[0] = inputBuffer;
    inBufferLength[0] = inputLength;

    if(aacDecoder_Fill(handle, inBuffer, inBufferLength, &valid) != AAC_DEC_OK)
    	return -1;

    if(aacDecoder_DecodeFrame(handle, (INT_PCM *)outputBuffer, outputLength, 0) != AAC_DEC_OK)
    	return -2;

    CStreamInfo *info = aacDecoder_GetStreamInfo(handle);

	return info->frameSize;

}

int Decoder::reset()
{
	if(handle!=NULL)
		aacDecoder_Close(handle);

	handle = NULL;
	this->init();
	return 0;
}
