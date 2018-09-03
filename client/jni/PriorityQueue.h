/*
 * PriorityQueue.h
 *
 *  Created on: Mar 16, 2016
 *      Author: scpark
 */

#ifndef PRIORITYQUEUE_H_
#define PRIORITYQUEUE_H_
#include <list>
#include <hash_map>
#include <pthread.h>
#include <stdint.h>
#include "LogHelper.h"
#include "AudioTool.h"
#include <math.h>

using namespace std;

class PriorityItem
{
public:
	PriorityItem(int seqNumber, uint8_t* buffer, int length)
	{
		this->seqNumber = seqNumber;
		this->buffer = (uint8_t*) malloc(length);
		memcpy(this->buffer, buffer, length);
		this->length = length;
	}

	virtual ~PriorityItem()
	{
		free(buffer);
	}

	int seqNumber;
	uint8_t* buffer;
	int length;

};

typedef hash_map<int, bool> SeqNumberMap;
typedef list<PriorityItem*> PriorityList;


class PriorityQueue {
public:
	PriorityQueue(int queueSize);
	virtual ~PriorityQueue();

	bool put(int seqNumber, uint8_t* buffer, int length);
	int get(uint8_t* buffer, int length);
	void reset();
	int getQueueCount();
	double getDeviation(){return deviation;}
	double getAverage(){return average;}

private:
	PriorityList ownList;
	int QUEUE_SIZE;
	SeqNumberMap gotNumberMap;
	pthread_mutex_t mutex;
	long insertTime = 0;
	long maxInsertGap = 0;
	double deviation = 0;
	double variance = 0;
	double average = 0;
	bool lock = false;
};

#endif /* PRIORITYQUEUE_H_ */
