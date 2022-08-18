package mypool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TrivialPool {
  public static void delay() {
    try {
      Thread.sleep((int)(Math.random() * 2000) + 1000);
    } catch (InterruptedException ie) {
      System.out.println("Huh? not expected!");
    }
  }
  public static void main(String[] args) throws Throwable {
    BlockingQueue<Runnable> taskQueue = new ArrayBlockingQueue<>(10);
    Runnable threadPoolRunnable = () -> {
      System.out.println("Worker starting");
      Runnable r = null;
      try {
        r = taskQueue.take();
      } catch (InterruptedException e) {
        System.out.println("odd, got an interrupt");
      }
      r.run();
      System.out.println("Worker completed a task, going round for more work");
    };

    new Thread(threadPoolRunnable).start();
    new Thread(threadPoolRunnable).start();

    Runnable t1 = () -> {
      System.out.println("task 1 starting");
      delay();
      System.out.println("task 1 ending");
    };
    Runnable t2 = () -> {
      System.out.println("second task starting");
      delay();
      System.out.println("second task ending");
    };

    taskQueue.put(t1);
    taskQueue.put(t2);
  }
}
