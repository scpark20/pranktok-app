package voice;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VoiceDataQueue<E> implements Queue<E> {
	private Queue<E> ownQueue;
	int QUEUE_NUMBER;
	
	public VoiceDataQueue(int queueNumber)
	{
		this.QUEUE_NUMBER = queueNumber;
		this.ownQueue = new ConcurrentLinkedQueue<E>();
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		// TODO Auto-generated method stub
		return ownQueue.addAll(arg0);
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
		ownQueue.clear();
	}

	@Override
	public boolean contains(Object object) {
		// TODO Auto-generated method stub
		return ownQueue.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return ownQueue.containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return ownQueue.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return ownQueue.iterator();
	}

	@Override
	public boolean remove(Object object) {
		// TODO Auto-generated method stub
		return ownQueue.remove(object);
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return ownQueue.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		// TODO Auto-generated method stub
		return ownQueue.retainAll(arg0);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return ownQueue.size();
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return ownQueue.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		// TODO Auto-generated method stub
		return ownQueue.toArray(array);
	}

	@Override
	public boolean add(E e) {
		// TODO Auto-generated method stub
		if(ownQueue.size()>this.QUEUE_NUMBER)
			ownQueue.poll();
		
		return ownQueue.add(e);
	}

	@Override
	public E element() {
		// TODO Auto-generated method stub
		return ownQueue.element();
	}

	@Override
	public boolean offer(E e) {
		// TODO Auto-generated method stub
		if(ownQueue.size()>this.QUEUE_NUMBER)
			ownQueue.clear();
		
		return ownQueue.offer(e);
	}

	@Override
	public E peek() {
		// TODO Auto-generated method stub
		return ownQueue.peek();
	}

	@Override
	public E poll() {
		// TODO Auto-generated method stub
		return ownQueue.poll();
	}

	@Override
	public E remove() {
		// TODO Auto-generated method stub
		return ownQueue.remove();
	}

}
