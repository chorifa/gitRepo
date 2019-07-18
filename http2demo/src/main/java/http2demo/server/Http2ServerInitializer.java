package http2demo.server;

import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import io.netty.handler.codec.http2.Http2CodecUtil;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2ServerUpgradeCodec;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AsciiString;
import io.netty.util.ReferenceCountUtil;

public class Http2ServerInitializer extends ChannelInitializer<SocketChannel> {

    private static final HttpServerUpgradeHandler.UpgradeCodecFactory upgradeCodecFactory = charSequence -> {
        if(AsciiString.contentEquals(Http2CodecUtil.HTTP_UPGRADE_PROTOCOL_NAME, charSequence)){
            return new Http2ServerUpgradeCodec(
                    Http2FrameCodecBuilder.forServer().build(),
                    new Http2MultiplexHandler(new BusinessHttp2Handler()));
        }
        else return null;
    };


    private final SslContext sslCtx;
    private final int maxHttpContentLength;

    Http2ServerInitializer(SslContext sslContext){
        this(sslContext, 16*1024);
    }

    Http2ServerInitializer(SslContext sslCtx, int maxHttpContentLength) {
        if (maxHttpContentLength < 0) {
            throw new IllegalArgumentException("maxHttpContentLength (expected >= 0): " + maxHttpContentLength);
        }
        this.sslCtx = sslCtx;
        this.maxHttpContentLength = maxHttpContentLength;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) {
        if(sslCtx != null) configureSsl(socketChannel);
        else configureClearText(socketChannel);
    }

    private void configureSsl(SocketChannel ch){
        ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()), new Http2OrHttpHandler());
    }

    private void configureClearText(SocketChannel ch){
        final HttpServerCodec sourceCodec = new HttpServerCodec();
        ch.pipeline()
                .addLast(sourceCodec)
                .addLast(new HttpServerUpgradeHandler(sourceCodec, upgradeCodecFactory))
                .addLast(new SimpleChannelInboundHandler<HttpMessage>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, HttpMessage msg) throws Exception {
                        // If this handler is hit then no upgrade has been attempted and the client is just talking HTTP.
                        System.err.println("Directly talking: " + msg.protocolVersion() + " (no upgrade was attempted)");
                        ChannelPipeline pipeline = ctx.pipeline();
                        pipeline.addAfter(ctx.name(), null, new BusinessHttpHandler("Direct. No Upgrade Attempted."));
                        pipeline.replace(this, null, new HttpObjectAggregator(maxHttpContentLength));
                        ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
                    }
                })
                .addLast(new UserEventLogger());
    }

    private static class UserEventLogger extends ChannelInboundHandlerAdapter{
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            System.out.println("User Event Triggered: "+evt);
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            // do not close
        }
    }

}
