package eu.javaspecialists.performance.managedblocker;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

// demo1: test100_000_000() time = 46832
// demo2: test100_000_000() time = 25230
// demo3: test100_000_000() time = 16822
// demo4: test100_000_000() time = 10226
// demo5: test100_000_000() time = 7016
// demo6: test100_000_000() time = 7212



// TODO: Sign up to Heinz's "The Java Specialists' Newsletter":
// TODO: tinyurl.com/gdansk17
// TODO: (Already signed up?  Say "hi" on same link)
public class Fibonacci {
    public static final Set<Thread> threads = ConcurrentHashMap.newKeySet();

    public BigInteger f(int n) {
        threads.clear();
        try {
            Map<Integer, BigInteger> cache = new ConcurrentHashMap<>();
            cache.put(0, BigInteger.ZERO);
            cache.put(1, BigInteger.ONE);
            return f(n, cache);
        } finally {
            System.out.println("Number of threads worked: " + threads.size());
        }
    }

    private final BigInteger RESERVED = BigInteger.valueOf(-1000);

    public BigInteger f(int n, Map<Integer, BigInteger> cache) {
        threads.add(Thread.currentThread());

        BigInteger result = cache.putIfAbsent(n, RESERVED);
        if (result == null) { // we won the race, can start working
            int half = (n + 1) / 2;

            RecursiveTask<BigInteger> f0_task = new RecursiveTask<BigInteger>() {
                protected BigInteger compute() {
                    return f(half - 1, cache);
                }
            };
            f0_task.fork();
            BigInteger f1 = f(half, cache);
            BigInteger f0 = f0_task.join();

            long time = n > 10_000 ? System.currentTimeMillis() : 0;
            try {
                if (n % 2 == 1) {
                    result = f0.multiply(f0).add(f1.multiply(f1));
                } else {
                    result = f0.shiftLeft(1).add(f1).multiply(f1);
                }
                synchronized (RESERVED) {
                    cache.put(n, result);
                    RESERVED.notifyAll();
                }
            } finally {
                time = n > 10_000 ? System.currentTimeMillis() - time : 0;
                if (time > 50) {
                    System.out.printf("f(%d) took %d ms%n", n, time);
                }
            }
        } else if (result == RESERVED) {
            // we must wait
            try {
                ReservedBlocker blocker = new ReservedBlocker(n, cache);
                ForkJoinPool.managedBlock(blocker);
                result = blocker.result;
            } catch (InterruptedException e) {
                throw new CancellationException("interrupted");
            }
        }
        return result;
    }

    private class ReservedBlocker implements ForkJoinPool.ManagedBlocker {
        private BigInteger result;
        private final int n;
        private final  Map<Integer, BigInteger> cache;

        public ReservedBlocker(int n, Map<Integer, BigInteger> cache) {
            this.n = n;
            this.cache = cache;
        }

        public boolean isReleasable() {
            return (result = cache.get(n)) != RESERVED;
        }

        public boolean block() throws InterruptedException {
            synchronized (RESERVED) {
                while(!(isReleasable())) {
                    RESERVED.wait();
                }
            }
            return true;
        }
    }
}
