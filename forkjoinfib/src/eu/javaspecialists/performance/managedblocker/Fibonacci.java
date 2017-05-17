package eu.javaspecialists.performance.managedblocker;

import java.math.*;
import java.util.*;
import java.util.concurrent.*;

// demo1: test100_000_000() time = 46832
// demo2: test100_000_000() time = 25230
// demo3: test100_000_000() time = 16822

// TODO: Sign up to Heinz's "The Java Specialists' Newsletter":
// TODO: tinyurl.com/gdansk17
// TODO: (Already signed up?  Say "hi" on same link)
public class Fibonacci {
    public static final Set<Thread> threads = ConcurrentHashMap.newKeySet();

    public BigInteger f(int n) {
        threads.add(Thread.currentThread());
        if (n == 0) return BigInteger.ZERO;
        if (n == 1) return BigInteger.ONE;

        int half = (n + 1) / 2;

        RecursiveTask<BigInteger> f0_task = new RecursiveTask<BigInteger>() {
            protected BigInteger compute() {
                return f(half - 1);
            }
        };
        f0_task.fork();
        BigInteger f1 = f(half);
        BigInteger f0 = f0_task.join();

        long time = n > 10_000? System.currentTimeMillis() : 0;
        try {
            if (n % 2 == 1) {
                return f0.multiply(f0).add(f1.multiply(f1));
            } else {
                return f0.shiftLeft(1).add(f1).multiply(f1);
            }
        } finally {
            time = n > 10_000 ? System.currentTimeMillis() - time : 0;
            if (time > 50) {
                System.out.printf("f(%d) took %d ms%n", n, time);
            }
        }
    }
}
