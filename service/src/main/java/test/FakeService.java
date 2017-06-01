package test;

import com.google.inject.Singleton;
import com.twitter.util.Future;
import com.twitter.util.FuturePool;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static scala.compat.java8.JFunction.func;

/**
 * This is a fake client to an RPC service in order to demonstrate how to making "blocking" calls to RPC servers.
 * For a real RPC service, you want to use Finagle library to create a client of the service. You get the benefit
 * of having a non-blocking client too.
 */
@Singleton
public class FakeService {
    private final FuturePool futurePool;

    @Inject
    public FakeService(FuturePool futurePool) {
        this.futurePool = futurePool;
    }

    public Future<Boolean> someCall(String name) {
        return futurePool.apply(func(() -> {
                // This is to mimic a blocking RPC request a remote service.
                try {
                    Thread.currentThread().sleep(500);
                } catch (InterruptedException e) {
                    // This is not production code. It's ok if we are interrupted.
                }
                // This check is for demonstration purpose only.
                return name.length() >= 5;
            }));
    }

    public Future<List<Integer>> someOtherCall(String id) {
        return futurePool.apply(func(() -> {
            // This is to mimic a blocking RPC request a remote service.
            try {
                Thread.currentThread().sleep(500);
            } catch (InterruptedException e) {
                // This is not production code. It's ok if we are interrupted.
            }
            return Arrays.asList(100);
        }));
    }
}
