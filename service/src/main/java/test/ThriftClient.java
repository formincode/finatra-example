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
        ServiceInterface.ServiceIface client = Thrift.client().newIface("localhost:1234", ServiceInterface.ServiceIface.class);
        Response resp = Await.result(client.get(new Request()));
        return;
    }
}
