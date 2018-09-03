

#ifndef Header_AudioPool_AudioTool
#define Header_AudioPool_AudioTool

#include <math.h>
#include <string>
#include <time.h>
#include <stdlib.h>
#include <sys/time.h>
#include <sys/limits.h>

#define INT_MAXAMP 65536.0f // (INTMAX / 2 / FFT_SIZE) (2^31 / 2^12)
#define SHORT_MAXAMP 32768.0f

#define LEFT_INDEX 0
#define RIGHT_INDEX 1

#define allocateStereoBuffer(x, length) \
		{ \
			x = (float**) malloc(sizeof(float*) * 2); \
			x[LEFT_INDEX] = (float*) malloc(sizeof(float) * length); \
			x[RIGHT_INDEX] = (float*) malloc(sizeof(float) * length); \
		}

#define allocateMonoBuffer(x, length) \
		{ \
			x = (float*) malloc(sizeof(float) * length); \
		}

#define allocateStereoDoubleBuffer(x, length) \
		{ \
			x = (double**) malloc(sizeof(double*) * 2); \
			x[LEFT_INDEX] = (double*) malloc(sizeof(double) * length); \
			x[RIGHT_INDEX] = (double*) malloc(sizeof(double) * length); \
		}

#define releaseStereoBuffer(x) \
		{ \
    		free(x[LEFT_INDEX]); \
    		free(x[RIGHT_INDEX]); \
    		free(x); \
		}

#define releaseMonoBuffer(x) \
		{ \
    		free(x); \
		}

class AudioTool {
public:
	static  void float2short(float *floatbuffer, short *shortbuffer, unsigned int length);
	static  void short2float(short *shortBuffer, float *floatBuffer, unsigned int length);
	static  void short2int(short *shortbuffer, int *intbuffer, unsigned int length, float maxAmplitude = INT_MAXAMP);

	/* resampling */
	static  int getNeedLength2resample(int oriLength, int oriSampleRate, int dstSampleRate);

	static  void resample(float *oriBuffer, int oriSampleLength, int oriSampleRate, float *dstBuffer, int dstSampleLength, int dstSampleRate);

	static  float vel2FloatScale(int velocity);
	static  float userVolumeScale(float volume);
	static const short int compressedValue[];

	static  float getSampleByFloatIndex(float *buffer, int bufferSampleLength, double index, bool left);
	static  int getMonoSampleByFloatIndex2(float *buffer, int bufferSampleLength, double sampleIndex, float *value);
	static  int getSampleByFloatIndex2(float *buffer, int bufferSampleLength, double sampleIndex, float *leftValue, float *rightValue);
	static  int ms2samples(float time, float sampleRate) {return (int) ((float)time * sampleRate / 1000.0f + 0.5f);}
	static  double samples2ms(long samples, float sampleRate) {return (double)samples / sampleRate * 1000.0f;}
	static  int getRingBufferPosition(int position, int bufferLength);
	static  double getRingBufferFloatPosition(double position, int bufferLength);
	static  long getSystemCurrentTime();
	static long getMicroCurrentTime();
    

    template<typename T>
    inline static int separateChannels(T *sourceBuffer, T *leftDestBuffer, T *rightDestBuffer, int bufferSampleLength)
    {
    	for(int i=0;i<bufferSampleLength;i++)
    	{
    		leftDestBuffer[i] = sourceBuffer[i*2];
    		rightDestBuffer[i] = sourceBuffer[i*2+1];
    	}
    }

    inline static float getMagnitude(float real, float imag)
    {
    	return sqrtf(real * real + imag * imag);
    }

    inline static float getPhase(float real, float imag)
	{
		return atan2f(imag, real);
	}

    inline static float wrapPhase(float phase)
    {
    	return phase - floorf((phase + M_PI) / (2.0f * M_PI)) * 2.0f * M_PI;
    }

    static int unwrapPhase(float *phaseArray, int length);


    inline static int bufferMultiply(float *destBuffer, float *sourceBuffer1, float *sourceBuffer2, int length)
    {
    	for(int i=0;i<length;i++)
    		destBuffer[i] = sourceBuffer1[i] * sourceBuffer2[i];

    	return 0;
    }

    inline static int putWindowBuffer(float *windowBuffer, int length)
    {
    	for(int i=0;i<length;i++)
    		windowBuffer[i] = 0.5f * (1.0f - cosf(2.0f * M_PI * i / (length - 1.0f)));

    	return 0;
    }

private:

	/* resampling */
	static  float getResamplingIndex(int oriIndex, int oriSampleRate, int dstSampleRate);
	inline static  int sampleIndex2bufferIndex(int sampleIndex, bool left);

	static const float vel2FloatScaleTable[];
	static const float userVolScaleTable[];

};

#endif
