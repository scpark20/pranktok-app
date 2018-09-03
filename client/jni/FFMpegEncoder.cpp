/*
 * Encoder.cpp
 *
 *  Created on: Mar 5, 2016
 *      Author: scpark
 */

#include "Encoder.h"

Encoder::Encoder() {
	// TODO Auto-generated constructor stub

}

Encoder::~Encoder() {
	// TODO Auto-generated destructor stub

}

bool Encoder::init(int sampleRate, int bitRate)
{
	this->SAMPLE_RATE = sampleRate;
	this->BIT_RATE;

	avcodec_register_all();
	//LOGI("%s1",__FUNCTION__);
	codec = avcodec_find_encoder(AV_CODEC_ID_AAC);
	if(codec==NULL)
		return false;
	//LOGI("%s2",__FUNCTION__);
	codecContext = avcodec_alloc_context3(codec);
	if(codecContext==NULL)
		return false;
	//LOGI("%s3 %d %d",__FUNCTION__, sampleRate, bitRate);
	codecContext->bit_rate = 64000;
	codecContext->sample_rate = sampleRate;
	codecContext->channels = 1;
	//codecContext->strict_std_compliance = FF_COMPLIANCE_EXPERIMENTAL;
	//codecContext->channel_layout = av_get_default_channel_layout(codecContext->channels);

	codecContext->sample_fmt = AV_SAMPLE_FMT_FLTP;
	codecContext->profile = FF_PROFILE_AAC_LOW;

	codecContext->codec_type = AVMEDIA_TYPE_AUDIO;

	if(avcodec_open2(codecContext, codec, NULL)<0)
		return false;
	//LOGI("%s4",__FUNCTION__);

	av_init_packet(&avPacket);
	avFrame = av_frame_alloc();
	return true;
}

int Encoder::put(uint8_t* inputBuffer, int inputLength,  uint8_t* outBuffer, int outputLength)
{
	int frameBytes;

	int gotPacket;


	avFrame->nb_samples = inputLength/2;
	avFrame->format = AV_SAMPLE_FMT_S16;
	avFrame->channels = codecContext->channels;
	float floatBuffer[16384];
	AudioTool::short2float((short*)inputBuffer, floatBuffer, inputLength/2);
	avFrame->data[0] = (uint8_t*)floatBuffer;

	int n = 0;
	if(avcodec_encode_audio2(codecContext, &avPacket, avFrame, &gotPacket)<0)
		return 0;

	if(gotPacket)
	{
		//LOGI("Encoder %s %d",__FUNCTION__, avPacket.size);
		memcpy(outBuffer, avPacket.data, avPacket.size);
		n = avPacket.size;
	}


	return n;
}

void Encoder::reset()
{
	av_frame_free(&avFrame);
	init(SAMPLE_RATE, BIT_RATE);

}
