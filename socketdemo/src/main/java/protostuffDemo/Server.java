package protostuffDemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class Server {

    // private volatile int cnt;

    private AtomicInteger atoCnt = new AtomicInteger(0);

    void init(int port) throws Exception{
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try{
            ServerBootstrap sbs = new ServerBootstrap();
            sbs.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG,1024)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ByteBuf delimiters = Unpooled.copiedBuffer("$$".getBytes());
                            socketChannel.pipeline()//.addLast(new LineBasedFrameDecoder(1024))
                                    //.addLast(new LineEncoder(LineSeparator.DEFAULT))
                                    .addLast(new DelimiterBasedFrameDecoder(1024, delimiters))
                                    .addLast(new ProtostuffDecoder(RequestExample.class))
                                    .addLast(new ProtostuffEncoder(RequestExample.class))
                                    //.addLast(new StringDecoder(StandardCharsets.UTF_8))
                                    //.addLast(new StringEncoder(StandardCharsets.UTF_8))
                                    .addLast(new Server.ServerHandler());
                        }
                    });
            ChannelFuture channelFuture = sbs.bind(port).sync();
            System.out.println("server bind done. try to do sth.");
            channelFuture.channel().closeFuture().sync(); // wait for closing server
        }finally {
            System.out.println("server close...");
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }

    private class ServerHandler extends ChannelInboundHandlerAdapter{
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
//            int i;
//            synchronized (Server.class){
//                i = ++cnt;
//            }
            RequestExample requestExample = (RequestExample) msg;
            System.out.println(atoCnt.addAndGet(1)+": server 接受到msg: "+msg);
            requestExample.setRequestTime(new Date());
            ctx.writeAndFlush(requestExample);
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.out.println("server read done. flush");
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
