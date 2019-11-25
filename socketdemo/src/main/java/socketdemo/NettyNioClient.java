package socketdemo;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.nio.charset.StandardCharsets;

class NettyNioClient {

    private EventLoopGroup group = new NioEventLoopGroup(2);

    public EventLoopGroup getGroup() {
        return group;
    }

    void connect(int port, String host, String order) throws Exception{
        Bootstrap bs = new Bootstrap();
        bs.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ByteBuf delimiters = Unpooled.copiedBuffer("$$".getBytes());
                        socketChannel.pipeline()//.addLast(new LineBasedFrameDecoder(512))
                                //.addLast(new LineEncoder(LineSeparator.DEFAULT))
                                .addLast(new DelimiterBasedFrameDecoder(512, delimiters))
                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new StringEncoder(StandardCharsets.UTF_8))
                                // StringEncoder和LineEncoder功能有重复
                                .addLast(new NioClient(order));
                    }
                });
        ChannelFuture channelFuture = bs.connect(host, port).sync();
        System.out.println("client connect done. try to do sth. ->> sleep 30s");
        Thread.sleep(10 * 1000); // 30s
        System.out.println("channel close");
        channelFuture.channel().close();
    }

    private static class NioClient extends ChannelInboundHandlerAdapter {

        private String order;

        NioClient(String order) {
            this.order = order;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            // 由于加了lineDecoder需要手动给出分隔符
            /*
            byte[] bytes = (order+System.getProperty("line.separator")).getBytes();
            ByteBuf byteBuf = Unpooled.buffer(bytes.length);
            byteBuf.writeBytes(bytes);
             */
            // 给了String encoder可以不用转为byteBuf
            // 如果不用LineEncoder，写的时候还需要手动添加换行符
            System.out.println("client : channel active");
//            System.out.println("client write and flush order: "+order);
//            ctx.writeAndFlush(order+"$$");
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            /*
            ByteBuf byteBuf = (ByteBuf)msg;
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(bytes);
            String ans = new String(bytes, StandardCharsets.UTF_8);
             */
            String ans = (String) msg;
            System.out.println("client get answer: "+ans);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            ctx.close();
        }
    }
}
