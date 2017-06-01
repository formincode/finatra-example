package test;

import com.google.inject.Provides;
import com.twitter.inject.TwitterModule;
import com.twitter.util.FuturePool;
import com.twitter.util.FuturePools;

import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by msrivastava on 6/1/17.
 */
public class DefaultModule extends TwitterModule {

    @Singleton
    @Provides
    public FuturePool createFuturePool() {
        ExecutorService executors = Executors.newFixedThreadPool(10);
        FuturePool futurePool = FuturePools.newInterruptibleFuturePool(executors);
        return futurePool;
    }
}
