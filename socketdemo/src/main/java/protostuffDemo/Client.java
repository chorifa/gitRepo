package protostuffDemo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class Client {

    private AtomicInteger cnt = new AtomicInteger(0);

    void connect(int port, String host) throws Exception{
        EventLoopGroup group = new NioEventLoopGroup();
        try{
            Bootstrap bs = new Bootstrap();
            bs.group(group).channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ByteBuf delimiters = Unpooled.copiedBuffer("$$".getBytes());
                            socketChannel.pipeline()//.addLast(new LineBasedFrameDecoder(1024))
                                    //.addLast(new LineEncoder(LineSeparator.DEFAULT))
                                    .addLast(new DelimiterBasedFrameDecoder(1024, delimiters))
                                    .addLast(new ProtostuffDecoder(RequestExample.class))
                                    .addLast(new ProtostuffEncoder(RequestExample.class))
                                    //
                                    //.addLast(new StringDecoder(StandardCharsets.UTF_8))
                                    //.addLast(new StringEncoder(StandardCharsets.UTF_8))
                                    // StringEncoder和LineEncoder功能有重复
                                    .addLast(new Client.ClientHandler());
                        }
                    });
            ChannelFuture channelFuture = bs.connect(host,port).sync();
            System.out.println("client connect done. try to do sth.");
            channelFuture.channel().closeFuture().sync();
        }finally {
            System.out.println("client close...");
            group.shutdownGracefully();
        }
    }

    private class ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            for(int i = 0 ; i < 300; i++) {
                RequestExample requestExample = new RequestExample();
                requestExample.setRequestTime(new Date());
                requestExample.setRequestId(123456L);
                requestExample.setRequestMethod("method1");
                ctx.writeAndFlush(requestExample);
                System.out.println("send request: " + requestExample);
            }
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println("get response: "+ (RequestExample) msg);
            if(cnt.addAndGet(1) == 300)
                ctx.close();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
