package test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.twitter.finagle.Thrift;
import com.twitter.finatra.http.AbstractHttpServer;
import com.twitter.finatra.http.filters.CommonFilters;
import com.twitter.finatra.http.routing.HttpRouter;

import java.util.Collection;

/**
 * Created by msrivastava on 5/31/17.
 */
public class ExampleServer extends AbstractHttpServer {

    @Override
    public String defaultFinatraHttpPort() {
        return ":8080";
    }

    @Override
    public Collection<Module> javaModules() {
        return ImmutableList.of(new DefaultModule());
    }

    @Override
    public void configureHttp(HttpRouter httpRouter) {
        httpRouter
                .filter(CommonFilters.class)
                .add(ExampleControllerJava.class);

        FakeService service = this.injector().instance(FakeService.class);
        Thrift.server().serveIface("localhost:1234",new ServiceIfaceImpl(service));
    }
}
