package test;

import com.twitter.util.Future;
import com.twitter.util.Futures;
import test.thriftjava.Request;
import test.thriftjava.Response;
import test.thriftjava.ServiceInterface;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

import static scala.compat.java8.JFunction.func;

/**
 * Created by msrivastava on 6/1/17.
 */
@Singleton
public class ServiceIfaceImpl implements ServiceInterface.ServiceIface {
    private FakeService fakeService;

    @Inject
    public ServiceIfaceImpl(FakeService fakeService) {
        this.fakeService=fakeService;
    }

    @Override
    public Future<Response> get(Request request) {
        Future<Boolean> call1 = fakeService.someCall("test");

        return call1.map(func(resultCall1 -> {return Arrays.asList(fakeService.someOtherCall(resultCall1.toString()));}))
                .flatMap(func(futures -> Futures.collect(futures)))
                .map(func(resultCall2 -> {return new Response();}));
    }
}
