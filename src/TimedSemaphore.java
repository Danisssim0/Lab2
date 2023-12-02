import java.util.LinkedList;
import java.util.Queue;

public class TimedSemaphore {
    private final int maxThreads;
    private final long timeout;
    private final Queue<Thread> waitingThreads;

    public TimedSemaphore(int maxThreads, long timeout) {
        this.maxThreads = maxThreads;
        this.timeout = timeout;
        this.waitingThreads = new LinkedList<>();
    }

    public synchronized void acquire() throws InterruptedException {
        // Если количество потоков достигло максимального значения, добавляем поток в очередь ожидания
        if (waitingThreads.size() >= maxThreads) {
            long startTime = System.currentTimeMillis();
            long elapsedTime = 0;

            // Пока не пройдет timeout и не освободится место в семафоре, ждем
            while (waitingThreads.size() >= maxThreads && elapsedTime < timeout) {
                wait(timeout - elapsedTime);
                elapsedTime = System.currentTimeMillis() - startTime;
            }
        }

        // Если пройден timeout и все места в семафоре заняты, выбрасываем исключение
        if (waitingThreads.size() >= maxThreads) {
            throw new InterruptedException("Превышено время ожидания разрешения");
        }

        // Разрешаем доступ к ресурсу
        waitingThreads.add(Thread.currentThread());
        while (waitingThreads.peek() != Thread.currentThread()) {
            wait();
        }
    }

    public synchronized void release() {
        // Освобождаем ресурс и будим следующий поток в очереди ожидания (если есть)
        waitingThreads.remove(Thread.currentThread());
        notifyAll();
    }

    public static void main(String[] args) {
        TimedSemaphore semaphore = new TimedSemaphore(3, 5000); // Максимум 3 потока, таймаут 5000 миллисекунд

        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                try {
                    semaphore.acquire();
                    System.out.println(Thread.currentThread().getName() + " получил разрешение");
                    Thread.sleep(2000); // Делаем какую-то работу
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    semaphore.release();
                    System.out.println(Thread.currentThread().getName() + " освободил разрешение");
                }
            }).start();
        }
    }
}
