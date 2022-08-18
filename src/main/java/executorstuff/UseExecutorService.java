package executorstuff;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

class MyCallable implements Callable<String> {
  private static int nextTaskId = 0;
  private int myTaskId = nextTaskId++;

  private static void delay() {
    try {
      Thread.sleep((int)(Math.random() * 2000) + 1000);
    } catch (InterruptedException e) {
      System.out.println("Shutdown requested!");
    }
  }

  public String call() throws Exception {
    System.out.println("Task " + myTaskId + " starting");
//    while(true) { // this would prevent the task ever completing,
    // which in turn prevents the pool ever shutting down!

      delay();
      if (Math.random() > 0.7) {
        throw new SQLException("DB busted");
      }
//    }
    System.out.println("Task " + myTaskId + " ending");
    return "Task " + myTaskId + " result value!";
  }
}

public class UseExecutorService {
  public static void main(String[] args) throws InterruptedException {
    final int TASK_COUNT = 6;
    ExecutorService es = Executors.newFixedThreadPool(2);
    List<Future<String>> handles = new ArrayList<>();

    for (int i = 0; i < TASK_COUNT; i++) {
      Future<String> handle = es.submit(new MyCallable());
      handles.add(handle);
    }
    Thread.sleep(500);
    handles.get(1).cancel(true);

    System.out.println("All tasks submitted");

    // 1) close the input queue
    // 2) wait for all tasks to complete naturally
    es.shutdown();

    // poll for completed tasks
    while (handles.size() > 0) {
      Iterator<Future<String>> iterator = handles.iterator();
      while (iterator.hasNext()) {
        Future<String> handle = iterator.next();
        if (handle.isDone()) {
          try {
            String result = handle.get();
            System.out.println("Foreground task got result: " + result);
          } catch (InterruptedException e) {
            System.out.println("surprise! foreground thread interrupted!");;
          } catch (ExecutionException e) {
            System.out.println("Task threw an exception " + e.getCause());;
          } catch (CancellationException ce) { // or handle.isCanceled()
            System.out.println("oops, that task was already canceled");
          }
          iterator.remove();
        }
      }
    }

    // 1) close the input queue
    // 2) dispose of all not-yet-started tasks
    // 3) send interrupt to all running tasks
    // 4) wait for those running tasks to complete
//    es.shutdownNow();

    System.out.println("All tasks completed");
  }
}
