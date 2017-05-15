package eu.javaspecialists.performance.managedblocker.issue223;

import java.util.stream.*;

// TODO: Sign up to Heinz's "The Java Specialists' Newsletter":
// TODO: tinyurl.com/gdansk17
// TODO: (Already signed up?  Say "hi" on same link)
public class PrintlnFun {
    public static void main(String... args) {
        synchronized (System.out) {
            System.out.println("Hello World");
            IntStream.range(0, 4).parallel().
                forEach(System.out::println);
        }
    }
}
