package protostuffDemo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ProtostuffDecoder extends ByteToMessageDecoder {

    private static byte[] delimiter = "$$".getBytes();

    private Class<?> genericClass;

    ProtostuffDecoder(Class<?> genericClass){
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
        if(byteBuf.readableBytes() < 4) return;
        byteBuf.markReaderIndex(); //
        int dataLength = byteBuf.readInt();
        if(dataLength < 0){
            channelHandlerContext.close();
            return;
        }
        if(byteBuf.readableBytes() < dataLength){
            byteBuf.resetReaderIndex();
            return;
        }
        System.out.println("start decode");
        byte[] data = new byte[dataLength];
        byteBuf.readBytes(data);
        Object obj = ProtostuffUtil.deserializer(data,genericClass);
        list.add(obj);
    }

}
