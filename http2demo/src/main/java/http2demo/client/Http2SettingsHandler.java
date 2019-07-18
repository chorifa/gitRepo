package http2demo.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http2.Http2Settings;

import java.util.concurrent.TimeUnit;

public class Http2SettingsHandler extends SimpleChannelInboundHandler<Http2Settings> {

    private final ChannelPromise promise;

    Http2SettingsHandler(ChannelPromise promise){
        this.promise = promise;
    }

    void awaitSettings(long timeout, TimeUnit unit){
        if(!promise.awaitUninterruptibly(timeout,unit))
            throw new IllegalStateException("Timed out waiting for settings");
        if(!promise.isSuccess())
            throw new RuntimeException(promise.cause());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Http2Settings http2Settings) throws Exception {
        promise.setSuccess();

        ctx.pipeline().remove(this);
    }
}
