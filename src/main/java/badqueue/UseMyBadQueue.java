package badqueue;

class BadQueue<E> {
  private static final int CAPACITY = 10;
  private E[] data = (E[])new Object[CAPACITY];
  private int count = 0;
  private Object rendezvous = new Object();

  public void put(E e) throws InterruptedException {
    synchronized (rendezvous) {
      while (count >= CAPACITY) {
        rendezvous.wait();
      }
      data[count++] = e;
      rendezvous.notify();
//      rendezvous.notifyAll(); // horribly non-scalable!!!
    }
  }

  public E take() throws InterruptedException {
    synchronized (rendezvous) {
      while (count <= 0) {
        rendezvous.wait();
      }

      E res = data[0];
      System.arraycopy(data, 1, data, 0, --count);
      rendezvous.notify();
//      rendezvous.notifyAll();
      return res;
    }
  }
}

public class UseMyBadQueue {
  public static void main(String[] args) throws Throwable {
    final BadQueue<int[]> bqi = new BadQueue<>();
    final long ITEM_COUNT = 50_000_000;

    Runnable producer = () -> {
      System.out.println("Producer starting...");
      try {
        for (int i = 0; i < ITEM_COUNT; i++) {
          int[] data = new int[]{0, i};
          if (i < 200) {
            Thread.sleep(1);
          }
          data[0] = i;

          if (i == 500) {
            data[0] = -1;
          }
          bqi.put(data);
          data = null;
        }
      } catch (InterruptedException ie) {
        System.out.println("surprising! asked to shut down");
      }
      System.out.println("Producer ending...");
    };

    Runnable cons = () -> {
      System.out.println("Consumer started...");
      try {
        for (int i = 0; i < ITEM_COUNT; i++) {
          int[] data = bqi.take();
          if (data[0] != data[1] || data[0] != i) {
            System.out.println("DATA ERROR !!!!");
          }
          if (i >= ITEM_COUNT - 200) {
            Thread.sleep(1); // force full queue for some period
          }
        }
      } catch (InterruptedException ie) {
        System.out.println("surprise! Consumer shutdown on request.");
      }
      System.out.println("Consumer ended...");
    };

    Thread pThread = new Thread(producer);
    Thread cThread = new Thread(cons);
    long start = System.nanoTime();
    pThread.start();
    cThread.start();
    System.out.println("test started...");
    pThread.join();
    cThread.join();
    long time = System.nanoTime() - start;
    System.out.printf("Time was %7.3f\n", (time / 1_000_000_000.0));
    System.out.printf("Throughput %7.3f items / second\n",
        ITEM_COUNT * 1_000_000_000.0 / time);
  }
}
