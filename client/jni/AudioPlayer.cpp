/*
 * AudioPlayer.cpp
 *
 *  Created on: Feb 19, 2016
 *      Author: scpark
 */

#include "AudioPlayer.h"

static bool bufferFlush;

void CheckErr( SLresult res )
{
    if ( res != SL_RESULT_SUCCESS )
    {
    // Debug printing to be placed herefassetManager
    	LOGE("%s CheckErr: %d", __FUNCTION__, res);
        return;
    }
}

AudioPlayer::AudioPlayer() {
	// TODO Auto-generated constructor stub

}

AudioPlayer::~AudioPlayer() {
	// TODO Auto-generated destructor stub
}

AudioPlayer* AudioPlayer::getInstance()
{
	if(instance==NULL)
		instance = new AudioPlayer();

	return instance;
}

void AudioPlayer::uninit()
{
	(*player)->Destroy(player);
	(*outputMix)->Destroy(outputMix);
	(*slEngine)->Destroy(slEngine);
}


void AudioPlayer::init(int systemSampleRate, int sampleRate, int bufferSize)
{

	SLresult res;
	SLEngineOption EngineOption[] = {
									(SLuint32) SL_ENGINEOPTION_THREADSAFE,
									(SLuint32) SL_BOOLEAN_TRUE};

	res = slCreateEngine( &slEngine, 1, EngineOption, 0, NULL, NULL);
	CheckErr(res);

	res = (*slEngine)->Realize(slEngine, SL_BOOLEAN_FALSE);



	/* Get the SL Engine Interface which is implicit */
	res = (*slEngine)->GetInterface(slEngine, SL_IID_ENGINE, (void *)&engineItf);
	CheckErr(res);
	const SLuint32 count = 1;
	const SLInterfaceID ids[count] = {SL_IID_ENVIRONMENTALREVERB};
	const SLboolean req[count] = {SL_BOOLEAN_FALSE};

	// Create Output Mix object to be used by player
	res = (*engineItf)->CreateOutputMix(engineItf, &outputMix, count, ids, req); CheckErr(res);

	// Realizing the Output Mix object in synchronous mode.
	res = (*outputMix)->Realize(outputMix, SL_BOOLEAN_FALSE); CheckErr(res);
	//res = (*OutputMix)->GetInterface(OutputMix, SL_IID_VOLUME, &volumeItf); CheckErr(res);
	/* Setup the data source structure for the buffer queue */

	SLDataLocator_AndroidSimpleBufferQueue loc_bufq =
	                           {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
	SLuint32 sr;

	switch(systemSampleRate)
	{
		case 44100:
			sr = SL_SAMPLINGRATE_44_1;
			break;
		case 48000:
			sr = SL_SAMPLINGRATE_48;
			break;
		case 64000:
			sr = SL_SAMPLINGRATE_64;
			break;
		case 88200:
			sr = SL_SAMPLINGRATE_88_2;
			break;
		case 96000:
			sr = SL_SAMPLINGRATE_96;
			break;
		case 192000:
			sr = SL_SAMPLINGRATE_192;
			break;
	}

	SLuint32 		formatType;
		SLuint32 		numChannels;
		SLuint32 		samplesPerSec;
		SLuint32 		bitsPerSample;
		SLuint32 		containerSize;
		SLuint32 		channelMask;
		SLuint32		endianness;

	SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM,1,sr,
	               SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
	               SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};
	SLDataSource audioSrc = {&loc_bufq, &format_pcm};

	SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMix};
	SLDataSink audioSnk = {&loc_outmix, NULL};


    /* Initialize the context for Buffer queue callbacks */
	context = (CallbackContext*) malloc(sizeof(CallbackContext));
	floatBuffer = (float*) malloc(sizeof(float) * 32768);


	context->byteBuffer1 = (uint8_t*) malloc(sizeof(uint8_t) * 32768);
	context->byteBuffer2 = (uint8_t*) malloc(sizeof(uint8_t) * 32768);
    context->floatBuffer = (float*) malloc(sizeof(float) * 32768);
    context->priorityQueue = new PriorityQueue(500);
	context->decoder = new Decoder();
	context->decoder->init();
	context->phaseVocoderMono = new PhaseVocoderMono(sampleRate, 8, bufferSize, 4);
	//LOGI("%s %d", __FUNCTION__, systemSampleRate);
	basePitchRate = (double)sampleRate / (double)systemSampleRate;
	basePlayRate = (double)sampleRate / (double)systemSampleRate;
	context->phaseVocoderMono->setPitchRate(basePitchRate);
	context->phaseVocoderMono->setBasePlayRate(basePlayRate);

    const SLuint32 arraySize = 3;
	const SLInterfaceID player_ids[arraySize] = { SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME, SL_IID_ANDROIDCONFIGURATION };
	const SLboolean player_req[arraySize] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE };

	/* Create the music player */
	res = (*engineItf)->CreateAudioPlayer(engineItf, &player, &audioSrc, &audioSnk, arraySize, player_ids, player_req); CheckErr(res);


	res = (*player)->GetInterface(player, SL_IID_ANDROIDCONFIGURATION, &playerConfig); CheckErr(res);

	SLint32 streamType = SL_ANDROID_STREAM_VOICE;
	res = (*playerConfig)->SetConfiguration(playerConfig, SL_ANDROID_KEY_STREAM_TYPE, &streamType, sizeof(SLint32)); CheckErr(res);

	/* Realizing the player in synchronous mode */
	res = (*player)->Realize(player, SL_BOOLEAN_FALSE); CheckErr(res);

	/* Get seek and play interfaces */
	res = (*player)->GetInterface(player, SL_IID_PLAY, (void *)&playItf); CheckErr(res);

	res = (*player)->GetInterface(player, SL_IID_VOLUME, (void *)&volumeItf); CheckErr(res);

	res = (*player)->GetInterface(player, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, (void*)&bufferQueueItf); CheckErr(res);


	/* Setup to receive buffer queue event callbacks */
	res = (*bufferQueueItf)->RegisterCallback(bufferQueueItf, AudioPlayer::bufferQueueCallback, context); CheckErr(res);

	/* Before we start set volume to -3dB (-300mB) */
	res = (*volumeItf)->SetVolumeLevel(volumeItf, 0); CheckErr(res);

}

static bool bufferCorrection = false;

void AudioPlayer::start()
{
	SLresult res;
	res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_PLAYING); CheckErr(res);
	bufferFlush = false;
	context->phaseVocoderMono->setPlayRate(1.0d);
	bufferCorrection = false;
	/* Enqueue a few buffers to get the ball rolling */
	(*bufferQueueItf)->Clear(bufferQueueItf);
	memset(context->byteBuffer1, 0x00, sizeof(short) * 128);
	res = (*bufferQueueItf)->Enqueue(bufferQueueItf, context->byteBuffer1, sizeof(short) * 128);
}

void AudioPlayer::stop()
{
	SLresult res;
	res = (*playItf)->SetPlayState( playItf, SL_PLAYSTATE_STOPPED); CheckErr(res);

	context->phaseVocoderMono->reset();
	context->priorityQueue->reset();
	context->decoder->reset();
}

void AudioPlayer::put(int seqNumber, uint8_t* buffer, int length)
{
	context->priorityQueue->put(seqNumber, buffer, length);
}



void AudioPlayer::bufferQueueCallback(SLBufferQueueItf queueItf, void *context)
{
	//LOGI("thread OpenslPlayer");
	SLresult res;
	CallbackContext *callbackContext = (CallbackContext*) context;
	PhaseVocoderMono *phaseVocoderMono = callbackContext->phaseVocoderMono;
	PriorityQueue *priorityQueue = callbackContext->priorityQueue;
	Decoder *decoder = callbackContext->decoder;

	int sampleLength = 256;

	uint8_t* byteBuffer1 = callbackContext->byteBuffer1;
	uint8_t* byteBuffer2 = callbackContext->byteBuffer2;
	float* floatBuffer = callbackContext->floatBuffer;

	const double timePerQueue = 21533.203125;
	const double timePerSamples = 22.675736961;
	int queueCount = priorityQueue->getQueueCount();
	double deviation = priorityQueue->getDeviation();
	int remainedSamples = phaseVocoderMono->getRemainedBufferSamples();

	double xMin = priorityQueue->getAverage();
	double xCenter = xMin + (4 * deviation);
	double xMax = xMin + (8 * deviation);

	double yMin = 0.9d;
	double yCenter = 1.0d;
	double yMax = 1.1d;

	double x = queueCount * timePerQueue + remainedSamples * timePerSamples;
	double playRate;

	if(x<xMin)
		playRate = yMin;
	else if(x>xMax)
		playRate = yMax;
	else if(x<xCenter)
	 	playRate = (1.0d - yMin) / (xCenter - xMin) * x + (1.0d - (1.0d - yMin) / (xCenter - xMin) * xCenter);
	else
		playRate = (yMax - 1.0d) / (xMax - xCenter) * x + (1.0d - (yMax - 1.0d) / (xMax - xCenter) * xCenter);

	phaseVocoderMono->setPlayRate(playRate);

	int n = phaseVocoderMono->get(floatBuffer, sampleLength);

	//LOGI("%s queueCount %d remainedSamples : %d x : %f playRate : %f n : %d", __FUNCTION__, queueCount, remainedSamples, x, playRate, n);

	if(n>0)
	{
		AudioTool::float2short(floatBuffer, (short*)byteBuffer1, n);
		goto ENQUEUE;
	}

	n = priorityQueue->get(byteBuffer1, 32768);

	if(n==0)
		goto BUFFER_INSUFFICIENT;

	n = decoder->decode(byteBuffer1, n, byteBuffer2, 32768);

	if(n<=0 || n > 32768)
	{
		//LOGI("%s %d", __FUNCTION__, n);
		goto BUFFER_INSUFFICIENT;
	}

	AudioTool::short2float((short*)byteBuffer2, floatBuffer, n);
	phaseVocoderMono->put(floatBuffer, n);
	n = phaseVocoderMono->get(floatBuffer, sampleLength);

	if(n==0)
		goto BUFFER_INSUFFICIENT;

	AudioTool::float2short(floatBuffer, (short*)byteBuffer1, n);

	goto ENQUEUE;


BUFFER_INSUFFICIENT:
	//LOGI("%s BUFFER_INSUFFICIENT", __FUNCTION__);
	memset(byteBuffer1, 0x00, sizeof(short) * sampleLength / 2);
	n = sampleLength / 2;

ENQUEUE:
	res = (*queueItf)->Enqueue(queueItf, (void*) byteBuffer1, sizeof(short) * n);
	CheckErr(res);
}
