//package com.acyumi.configuration.converter;
//
//import com.fasterxml.jackson.core.JsonFactory;
//import com.fasterxml.jackson.core.io.IOContext;
//import com.fasterxml.jackson.core.io.InputDecorator;
//import org.springframework.util.StreamUtils;
//
//import java.io.*;
//
///**
// * http请求信息输入流包装器.
// *
// * @author Mr.XiHui
// * @date 2018/3/22
// * @see JsonFactory#createParser(InputStream)
// */
//public class MsgInputDecorator extends InputDecorator {
//
//    private static final long serialVersionUID = -4383384685295065476L;
//
//    @Override
//    public InputStream decorate(IOContext ctxt, InputStream in) throws IOException {
//        //ByteArrayInputStream的close()方法什么都没干(因为它是内存输入流，GC会自动回收)
//        //所以就算被调用了close(),ByteArrayInputStream还是可以重复利用
//        return new ByteArrayInputStream(StreamUtils.copyToByteArray(in));
//        /*
//        ByteArrayInputStream bais = new ByteArrayInputStream(StreamUtils.copyToByteArray(in));
//        ByteArrayInputStream是支持mark的，起始mark位置为0，所以这里就注释掉了
//        if (bais.markSupported()) {
//            bais.mark(0);//查看源码可知: 无论mark的入参是什么ByteArrayInputStream都是记录当前已读取到的位置
//        }
//        return bais;
//        */
//    }
//
//    @Override
//    public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length) {
//        return new ByteArrayInputStream(src, offset, length);
//    }
//
//    @Override
//    public DataInput decorate(IOContext ctxt, DataInput input) {
//        //返回null让调用的地方按重写前的程序走
//        return null;
//    }
//
//    @Override
//    public Reader decorate(IOContext ctxt, Reader r) {
//        //返回null让调用的地方按重写前的程序走
//        return null;
//    }
//}
