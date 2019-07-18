package http2demo.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http2.*;

import java.nio.charset.StandardCharsets;

@ChannelHandler.Sharable
public class BusinessHttp2Handler extends ChannelDuplexHandler {

    private static ByteBuf HELLO_WORLD = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Hello World!", StandardCharsets.UTF_8));

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //super.exceptionCaught(ctx, cause);
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof Http2HeadersFrame){
            onHeaderRead(ctx, (Http2HeadersFrame) msg);
        }else if(msg instanceof Http2DataFrame){
            onDataRead(ctx, (Http2DataFrame) msg);
        }else{
            super.channelRead(ctx, msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("using http2 handler....");
        ctx.flush();
    }

    private static void onDataRead(ChannelHandlerContext ctx, Http2DataFrame data){
        if(data.isEndStream()){
            sendResponse(ctx,data.content());
        }else{
            data.release();
        }
    }

    private static void onHeaderRead(ChannelHandlerContext ctx, Http2HeadersFrame headers){
        if(headers.isEndStream()){
            ByteBuf content = ctx.alloc().buffer();
            content.writeBytes(HELLO_WORLD.duplicate());
            ByteBufUtil.writeAscii(content, " - via HTTP/2");
            sendResponse(ctx,content);
        }
    }

    private static void sendResponse(ChannelHandlerContext ctx, ByteBuf payload) {
        Http2Headers headers = new DefaultHttp2Headers().status(HttpResponseStatus.OK.codeAsText());
        ctx.write(new DefaultHttp2HeadersFrame(headers));
        ctx.write(new DefaultHttp2DataFrame(payload,true));
    }

}
