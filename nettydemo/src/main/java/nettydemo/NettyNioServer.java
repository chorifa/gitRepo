package nettydemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

class NettyNioServer {

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
                       ByteBuf delimiter = Unpooled.copiedBuffer("$$".getBytes());
                       socketChannel.pipeline()//.addLast(new LineBasedFrameDecoder(512))
                                               .addLast(new DelimiterBasedFrameDecoder(512,delimiter))
                                               .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                               .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                               .addLast(new NioServer());
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

    private class NioServer extends ChannelInboundHandlerAdapter{
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            /*
            ByteBuf byteBuf = (ByteBuf)msg;
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            String req = new String(bytes, StandardCharsets.UTF_8);
            */
            String req = (String) msg;
            System.out.println("server get order: "+req);
            String ans;
            switch (req){
                case "order time" :
                    ans = new Date().toString();
                    break;
                case "order name" :
                    ans = "chorifa";
                    break;
                default:
                    ans = "unsupported order";
            }
            // ByteBuf ansbuf = Unpooled.copiedBuffer(ans.getBytes());
            System.out.println("server write ans: "+ans);
            ctx.write(ans+"$$");
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            System.out.println("server read done. flush");
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
