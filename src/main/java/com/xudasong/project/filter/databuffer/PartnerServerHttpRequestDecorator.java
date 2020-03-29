package com.xudasong.project.filter.databuffer;

import com.google.common.collect.Lists;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import reactor.core.publisher.Flux;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;


@Slf4j
public class PartnerServerHttpRequestDecorator extends ServerHttpRequestDecorator {

    private byte[] bytes;

    public PartnerServerHttpRequestDecorator(ServerHttpRequest delegate) {
        super(delegate);
    }

    @Override
    public Flux<DataBuffer> getBody(){
        if (Objects.isNull(bytes) || bytes.length == 0){
            Flux<DataBuffer> flux = super.getBody();
            cache(flux);
            return flux;
        }else {
            return getBodyMore();
        }
    }

    private Flux<DataBuffer> getBodyMore(){
        return Flux.create(sink -> {
            NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(new UnpooledByteBufAllocator(false));
            DataBuffer dataBuffer = nettyDataBufferFactory.wrap(bytes);
            sink.next(dataBuffer);
            sink.complete();
        });
    }

    private void cache(Flux<DataBuffer> flux){
        List<DataBuffer> list = Lists.newArrayList();
        flux.doOnComplete(this::trace).map(list::add).publish();
        try (ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()) {
            for (DataBuffer buffer : list){
                try (InputStream inputStream = buffer.asInputStream()) {
                    IOUtils.copy(inputStream,arrayOutputStream);
                }
            }
            bytes = arrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("缓存数据包失败", e);
        }
    }

    private void trace(){
        log.info("缓存数据包完成，总长度：{}", bytes.length);
    }

}
