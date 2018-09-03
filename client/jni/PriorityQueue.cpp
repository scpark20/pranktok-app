/*
 * PriorityQueue.cpp
 *
 *  Created on: Mar 16, 2016
 *      Author: scpark
 */

#include "PriorityQueue.h"


PriorityQueue::PriorityQueue(int queueSize) {
	// TODO Auto-generated constructor stub
	this->QUEUE_SIZE = queueSize;
	pthread_mutex_init(&mutex, NULL);
}

PriorityQueue::~PriorityQueue() {
	// TODO Auto-generated destructor stub
	this->reset();
	pthread_mutex_destroy(&mutex);
}

bool PriorityQueue::put(int seqNumber, uint8_t* buffer, int length)
{

	SeqNumberMap::iterator mapIter = gotNumberMap.find(seqNumber);
	if(mapIter!=gotNumberMap.end())
		return false;

	PriorityList::iterator listIter = ownList.begin();
	while(listIter!=ownList.end())
	{
		PriorityItem *item = *listIter;
		if(item->seqNumber==seqNumber)
			return false;
		else if(item->seqNumber<seqNumber)
		{
			listIter++;
			continue;
		}
		else
			break;
	}

	PriorityItem* newItem = new PriorityItem(seqNumber, buffer, length);

	pthread_mutex_lock(&mutex);

		gotNumberMap.insert(SeqNumberMap::value_type(seqNumber, true));
		ownList.insert(listIter, newItem);

		if(ownList.size()>QUEUE_SIZE)
			ownList.pop_front();

		if(insertTime==0)
			insertTime = AudioTool::getMicroCurrentTime();
		else
		{
			long currentTime = AudioTool::getMicroCurrentTime();
			long currentGap = currentTime - insertTime;
			average = ((double)average * 0.99d) + ((double)currentGap * 0.01d);
			double currentVariance = (double)(currentGap - average);
			currentVariance *= currentVariance;
			variance = (variance * 0.99d) + (currentVariance *0.01d);
			deviation = sqrt(variance);
			insertTime = currentTime;
			if(maxInsertGap < currentGap)
				maxInsertGap = currentGap;

			//LOGI("%s %ld %ld %f %f", __FUNCTION__, currentGap, maxInsertGap, average, deviation);
		}



	pthread_mutex_unlock(&mutex);

	return true;
}

int PriorityQueue::get(uint8_t* buffer, int length)
{
	/*
	if(lock)
	{
		if(ownList.size()>10)
			lock = false;
		else
			return 0;
	}
	*/
	if(ownList.empty())
	{
		lock = true;
		return 0;
	}

	pthread_mutex_lock(&mutex);

	PriorityItem *item = (PriorityItem*) ownList.front();
	ownList.pop_front();
	pthread_mutex_unlock(&mutex);

	if(item->length > length)
		return 0;

	memcpy(buffer, item->buffer, item->length);
	int ret = item->length;
	delete item;

	return ret;
}

void PriorityQueue::reset()
{
	pthread_mutex_lock(&mutex);
		PriorityList::iterator iterEnd = ownList.end();

		for(PriorityList::iterator iter = ownList.begin();
				iter!=iterEnd; iter++)
		{
			PriorityItem* item = (PriorityItem*)*iter;
			delete item;
		}

		ownList.clear();
		gotNumberMap.clear();

		insertTime = 0;
		maxInsertGap = 0;
		deviation = 0;
		variance = 0;
		average = 0;
		lock = false;
	pthread_mutex_unlock(&mutex);
}

int PriorityQueue::getQueueCount()
{
	pthread_mutex_lock(&mutex);
		int ret = this->ownList.size();
	pthread_mutex_unlock(&mutex);
	return ret;
}
