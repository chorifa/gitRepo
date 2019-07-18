package http2demo.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_0;
import static io.netty.util.internal.ObjectUtil.checkNotNull;

public class BusinessHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String establishApproach;

    public BusinessHttpHandler(String establishApproach) {
        this.establishApproach = checkNotNull(establishApproach, "establishApproach");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        if(HttpUtil.is100ContinueExpected(req)){
            ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
        }
        boolean keepAlive = HttpUtil.isKeepAlive(req);

        ByteBuf content = ctx.alloc().buffer();
        content.writeBytes(req.content().duplicate());
        ByteBufUtil.writeAscii(content," - via " + req.protocolVersion() + " (" + establishApproach + ")");

        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());

        if(keepAlive){
            if (req.protocolVersion().equals(HTTP_1_0)) {
                response.headers().set(CONNECTION, KEEP_ALIVE);
            }
            ctx.write(response);
        }else {
            // Tell the client we're going to close the connection.
            response.headers().set(CONNECTION, CLOSE);
            ctx.write(response).addListener(ChannelFutureListener.CLOSE);
        }

    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
