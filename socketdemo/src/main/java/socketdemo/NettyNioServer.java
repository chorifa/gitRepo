package socketdemo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Date;

class NettyNioServer {

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workGroup = new NioEventLoopGroup(2);

    void shutdown(){
        bossGroup.shutdownGracefully();
        workGroup.shutdownGracefully();
        System.out.println("Server shut down.");
    }

    void init(int port) throws InterruptedException {
        ServerBootstrap sbs = new ServerBootstrap();
        sbs.group(bossGroup,workGroup).channel(NioServerSocketChannel.class)
           .option(ChannelOption.SO_BACKLOG,1024)
           .childHandler(new ChannelInitializer<SocketChannel>(){
               @Override
               protected void initChannel(SocketChannel socketChannel) throws Exception {
                   ByteBuf delimiter = Unpooled.copiedBuffer("$$".getBytes());
                   socketChannel.pipeline()//.addLast(new LineBasedFrameDecoder(512))
//                                           .addLast(new DelimiterBasedFrameDecoder(512,delimiter))
//                                           .addLast(new StringDecoder(StandardCharsets.UTF_8))
//                                           .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                           .addLast(new NioServer());
               }
           });
        ChannelFuture channelFuture = sbs.bind(port).sync();
        System.out.println("server bind done. try to do sth.");
    }

    private static class NioServer extends ChannelInboundHandlerAdapter{
        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // 这个时候已经断开了
            System.out.println("server detect: channel inactive.");
//            try {
//                Thread.sleep(20*1000); //20s
//            }catch (InterruptedException ignore){}
            super.channelInactive(ctx);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            try {
                System.out.println("do channel read.");
                Thread.sleep(20*1000); //20s
            }catch (InterruptedException ignore){}

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
            // 上边的read 没有触发
            try {
                System.out.println("do channel read complete."); // 这里还没有断开
                Thread.sleep(20*1000); //20s
            }catch (InterruptedException ignore){}
            System.out.println("server read done. channel active? " + ctx.channel().isActive());
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            //ctx.close();
        }
    }
}
