package test;

import com.twitter.util.Future;
import com.twitter.util.FuturePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.annotation.AnnotatedController;
import test.annotation.Get;
import test.annotation.Param;

import javax.inject.Inject;
import javax.inject.Singleton;

import static scala.compat.java8.JFunction.func;

/**
 * Created by msrivastava on 5/31/17.
 */
@Singleton
public class ExampleAnnotatedControllerJava extends AnnotatedController {
    private static final String ERROR_CONTENT_STRING = "{\"errors\":[\"%s\"]}";
    private FakeService fakeService;
    private FuturePool pool;
    private static final Logger logger = LoggerFactory.getLogger(ExampleAnnotatedControllerJava.class);


    @Inject
    public ExampleAnnotatedControllerJava(FakeService fakeService, FuturePool pool) {
        this.fakeService=fakeService;
        this.pool=pool;
    }

    @Get("/:userId/test.json")
    public Future<String> getTest(@Param("userId") String userId) {
        return pool.apply(func(() -> {
            return "hello "+userId;
        }));
    }

}
