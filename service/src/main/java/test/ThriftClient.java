package test;

import com.twitter.finagle.Thrift;
import com.twitter.util.Await;
import test.thriftjava.Request;
import test.thriftjava.Response;
import test.thriftjava.ServiceInterface;

/**
 * Created by msrivastava on 6/1/17.
 */
public class ThriftClient {
    public static void main(String[] args) throws Exception {
        String dest = "zk!localhost:2181!//test/example";
        ServiceInterface.ServiceIface client = Thrift.client().newIface(dest, ServiceInterface.ServiceIface.class);
        Response resp = Await.result(client.get(new Request()));

        client = Thrift.client().newIface("localhost:1234", ServiceInterface.ServiceIface.class);
        resp = Await.result(client.get(new Request()));
        return;
    }
}
