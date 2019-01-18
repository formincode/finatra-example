package test;

import com.twitter.finagle.http.Response;
import com.twitter.util.Future;
import com.twitter.util.FuturePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.annotation.AnnotatedController;
import test.annotation.Body;
import test.annotation.Get;
import test.annotation.Param;
import test.annotation.Post;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by msrivastava on 5/31/17.
 */
@Singleton
public class ExampleAnnotatedControllerJava extends AnnotatedController {
    private FakeService fakeService;
    private FuturePool pool;
    private static final Logger logger = LoggerFactory.getLogger(ExampleAnnotatedControllerJava.class);


    @Inject
    public ExampleAnnotatedControllerJava(FakeService fakeService, FuturePool pool) {
        this.fakeService=fakeService;
        this.pool=pool;
    }

    @Get("/:userId/test.json")
    public Future<Response> getTest(@Param("userId") String userId) {
        System.out.println("{\"hello\":"+userId+"}");
        return Future.value(response().ok().json("{\"hello\":"+userId+"}"));
    }

    @Post("/:userId/test2.json")
    public Future<Response> postTest(@Param("userId") String userId,@Body String value) {
        return Future.value(response().ok("hello"+value));
    }
}
