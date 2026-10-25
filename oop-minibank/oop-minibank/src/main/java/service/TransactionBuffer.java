package service;

/** Small bounded buffer: producer and consumer coordinate with wait()/notifyAll(). */
public class TransactionBuffer {
    private final String[] items;
    private int head = 0;
    private int tail = 0;
    private int count = 0;

    public TransactionBuffer(int capacity) {
        items = new String[capacity];
    }

    public synchronized void put(String item) throws InterruptedException {
        while (count == items.length) {
            wait();                       // buffer full: wait for the consumer
        }
        items[tail] = item;
        tail = (tail + 1) % items.length;
        count++;
        notifyAll();                      // wake the consumer
    }

    public synchronized String take() throws InterruptedException {
        while (count == 0) {
            wait();                       // buffer empty: wait for the producer
        }
        String item = items[head];
        head = (head + 1) % items.length;
        count--;
        notifyAll();                      // wake the producer
        return item;
    }
}
