/*
 * Decoder.cpp
 *
 *  Created on: Mar 5, 2016
 *      Author: scpark
 */

#include "Decoder.h"

Decoder::Decoder() {
	// TODO Auto-generated constructor stub

}

Decoder::~Decoder() {
	// TODO Auto-generated destructor stub
}

bool Decoder::init(int sampleRate)
{
	this->SAMPLE_RATE = sampleRate;
	this->BIT_RATE;

	//LOGI("%s1", __FUNCTION__);
	avcodec_register_all();
	//LOGI("%s2", __FUNCTION__);
	codec = avcodec_find_decoder(AV_CODEC_ID_AAC);
	if(codec==NULL)
		return false;
	//LOGI("%s2.5", __FUNCTION__);
	codecContext = avcodec_alloc_context3(codec);
	if(codecContext==NULL)
		return false;

	codecContext->bit_rate = 64000;
	codecContext->sample_rate = sampleRate;
	codecContext->channels = 1;
	//codecContext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
	//codecContext->channel_layout = av_get_default_channel_layout(codecContext->channels);
	codecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
	codecContext->profile = FF_PROFILE_AAC_LOW;

	codecContext->codec_type = AVMEDIA_TYPE_AUDIO;

	//LOGI("%s3", __FUNCTION__);
	if(avcodec_open2(codecContext, codec, NULL)<0)
		return false;
	//LOGI("%s4", __FUNCTION__);

	av_init_packet(&avPacket);
	avFrame = av_frame_alloc();
	return true;
}

int Decoder::put(uint8_t* inputBuffer, int inputLength,  uint8_t* outBuffer, int outputLength)
{
	int frameBytes;

	int gotFrame;

	avPacket.data = inputBuffer;
	avPacket.size = inputLength;

	int offset = 0;
	while(avPacket.size > 0) {
		gotFrame = 0;
		int len = avcodec_decode_audio4(codecContext, avFrame, &gotFrame, &avPacket);

		if(len<0)
			return 0;

		if(gotFrame)
		{
			AudioTool::float2short((float*)avFrame->data[0], (short*) &outBuffer[offset], avFrame->nb_samples);
			//memcpy(&outBuffer[offset], avFrame->data[0], sizeof(short) * avFrame->nb_samples);
			offset += avFrame->nb_samples;
		}

	    avPacket.size -= len;
	    avPacket.data += len;

	}

	//LOGI("Decoder %s2 %d", __FUNCTION__, n);
	return offset;
}

void Decoder::reset()
{
	av_frame_free(&avFrame);
	init(SAMPLE_RATE);
}
