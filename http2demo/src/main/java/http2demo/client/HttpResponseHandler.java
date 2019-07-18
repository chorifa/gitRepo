package http2demo.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.CharsetUtil;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

    private final Map<Integer, Map.Entry<ChannelFuture, ChannelPromise>> streamIDPromiseMap;

    HttpResponseHandler(){
        streamIDPromiseMap = new ConcurrentHashMap<>();
    }

    Map.Entry<ChannelFuture, ChannelPromise> put(int streamId, ChannelFuture writeFuture, ChannelPromise promise) {
        return streamIDPromiseMap.put(streamId, new AbstractMap.SimpleEntry<>(writeFuture, promise));
    }

    void awaitAllResponses(long timeout, TimeUnit unit){
        Iterator<Map.Entry<Integer, Map.Entry<ChannelFuture,ChannelPromise>>> itr = streamIDPromiseMap.entrySet().iterator();
        while (itr.hasNext()){
            Map.Entry<Integer, Map.Entry<ChannelFuture,ChannelPromise>> entry = itr.next();
            ChannelFuture writeFuture = entry.getValue().getKey();
            if (!writeFuture.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting to write for stream id " + entry.getKey());
            }
            if (!writeFuture.isSuccess()) {
                throw new RuntimeException(writeFuture.cause());
            }
            ChannelPromise promise = entry.getValue().getValue();
            if (!promise.awaitUninterruptibly(timeout, unit)) {
                throw new IllegalStateException("Timed out waiting for response on stream id " + entry.getKey());
            }
            if (!promise.isSuccess()) {
                throw new RuntimeException(promise.cause());
            }
            System.out.println("---Stream id: " + entry.getKey() + " received---");
            itr.remove();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        Integer streamId = msg.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text());
        if(streamId == null){
            System.err.println("HttpResponseHandler unexpected message received: " + msg);
            return;
        }

        Map.Entry<ChannelFuture, ChannelPromise> entry = streamIDPromiseMap.get(streamId);
        if(entry == null){
            System.err.println("Message received for unknown stream id " + streamId);
        }else{
            ByteBuf content = msg.content();
            if (content.isReadable()) {
                int contentLength = content.readableBytes();
                byte[] arr = new byte[contentLength];
                content.readBytes(arr);
                System.out.println(new String(arr, 0, contentLength, CharsetUtil.UTF_8));
            }

            entry.getValue().setSuccess();
        }
    }
}
