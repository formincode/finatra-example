package test;

import com.google.common.net.MediaType;
import com.twitter.finatra.http.AbstractController;
import com.twitter.util.Future;
import com.twitter.util.Futures;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

import static scala.compat.java8.JFunction.func;

/**
 * Created by msrivastava on 5/31/17.
 */
@Singleton
public class ExampleControllerJava extends AbstractController {
    private static final String ERROR_CONTENT_STRING = "{\"errors\":[\"%s\"]}";
    private FakeService fakeService;

    @Inject
    public ExampleControllerJava(FakeService fakeService) {
        this.fakeService=fakeService;
    }

    public void configureRoutes() {
        get("/ping.json",
                request ->  {
                    try{
                        Future<Boolean> call1Test = fakeService.someCall("test");

                        return call1Test
                             .map(func(resultCall1 -> {return Arrays.asList(fakeService.someOtherCall(resultCall1.toString()));}))
                             .flatMap(func(futures -> Futures.collect(futures)))
                             .map(func(resultCall2 -> {
                                     StringBuffer buffer = new StringBuffer();
                                     resultCall2.forEach(x ->  buffer.append(x.toString()).append(" "));
                                     return response().ok().body(buffer.toString()).contentType(MediaType.JSON_UTF_8);
                             }));
                    } catch (Exception e) {
                        String errorString=String.format(ERROR_CONTENT_STRING,e.getMessage());
                        return response().badRequest(errorString).contentType(MediaType.JSON_UTF_8);
                    }
                });
        get("/pong.json",
                request ->  {
                    try{
                        Future<Boolean> call1 = fakeService.someCall("test");
                        Future<Integer> call2 = fakeService.someOtherCall("test");

                        return Futures.join(call1, call2)
                                .map(func(tuple -> {
                                    Boolean resultCall1 = tuple._1();
                                    Integer resultCall2 = tuple._2();
                                    return response().ok().body(resultCall2.toString()).contentType(MediaType.JSON_UTF_8);
                                }));

                    } catch (Exception e) {
                        String errorString=String.format(ERROR_CONTENT_STRING,e.getMessage());
                        return response().badRequest(errorString).contentType(MediaType.JSON_UTF_8);
                    }
                });
    }
}
