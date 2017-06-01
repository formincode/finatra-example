namespace java test.thriftjava


struct testMessage {
	0: optional i64 userId
	2: optional string data
}

struct Request {
    1: optional testMessage msg
}

struct Response {
    1: optional string response
}

service ServiceInterface {
    Response get(1: Request request)
}
