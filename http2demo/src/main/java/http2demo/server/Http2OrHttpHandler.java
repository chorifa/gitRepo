package http2demo.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;

public class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {

    private static final int MAX_CONTENT_LENGTH = 1024 * 100;

    Http2OrHttpHandler(){
        super(ApplicationProtocolNames.HTTP_1_1);
    }

    @Override
    protected void configurePipeline(ChannelHandlerContext ctx, String protocol) throws Exception {
        switch (protocol){
            case ApplicationProtocolNames.HTTP_2:
                ctx.pipeline().addLast(Http2FrameCodecBuilder.forServer().build())
                        .addLast(new Http2MultiplexHandler(new BusinessHttp2Handler()));
                break;
            case ApplicationProtocolNames.HTTP_1_1:
                ctx.pipeline().addLast(new HttpServerCodec(),
                                       new HttpObjectAggregator(MAX_CONTENT_LENGTH),
                                       new BusinessHttpHandler("ALPN Negotiation"));
                break;
            default:
                throw new IllegalStateException("unsupported protocol: " + protocol);
        }

    }

}
