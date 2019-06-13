package protostuffDemo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProtostuffEncoder extends MessageToByteEncoder {

    private static byte[] delimiter = "$$".getBytes();

    private Class<?> genericClass;

    ProtostuffEncoder(Class<?> genericClass){
        this.genericClass = genericClass;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        if(genericClass.isInstance(o)){
            System.out.println("start encode");
            byte[] data = ProtostuffUtil.serializer(o);
            byteBuf.writeInt(data.length);
            byteBuf.writeBytes(data);
            byteBuf.writeBytes(delimiter);
        }
    }
}
