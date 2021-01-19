package br.com.test.application;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@Slf4j
@RequiredArgsConstructor
@Service
//TODO: Add logs
public class UploadFilter implements GlobalFilter, Ordered {
    private static final String OBJECT_STORAGE = "object-storage";
    private static final String BUCKET = "bucket";
    private static final String NAME = "name";
    private static final String DATA = "data";
    private ExecutorService executorService = Executors.newWorkStealingPool();
    @Override
    public int getOrder() {
        return 1;
    }
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        Function<String, String> getQueryParam = this.getQueryParam.apply(request);
        String bucket = !StringUtils.isEmpty(getQueryParam.apply(BUCKET)) ? getQueryParam.apply(BUCKET)
                : exchange.getAttribute(BUCKET);
        String name = !StringUtils.isEmpty(getQueryParam.apply(NAME)) ? getQueryParam.apply(NAME)
                : exchange.getAttribute(NAME);
        String contentType = Optional.ofNullable(request.getHeaders().getContentType()).map(MediaType::toString)
                // TODO: Handle null
                .orElse("");
        tryToWriteOnStream(exchange, bucket, name, contentType);
        return exchange.getResponse().setComplete();
    }
    protected void tryToWriteOnStream(ServerWebExchange exchange, String bucket, String name, String contentType) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            PipedInputStream pipedInputStream = new PipedInputStream(1024 * 1024 * 1024);
            PipedOutputStream pipedOutputStream = new PipedOutputStream(pipedInputStream);
            final MediaType mediaType = request.getHeaders().getContentType();
            if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)) {
                Consumer<Flux<byte[]>> writeOnStream = fb -> fb.subscribe(writeOnStream(pipedOutputStream));
                // @formatter:off
                exchange.getMultipartData()
                        .map(mpm->{
                            return mpm.get(DATA);
                        })
                        .flatMapMany(lp->{
                            return Flux.fromIterable(lp);
                        })
                        .map(this::partToBytes)
                        .doOnComplete(closeQuietly(pipedOutputStream))
                        .subscribe(writeOnStream);
                // @formatter:on
            } else {
                // TODO: Fix pipe
                Consumer<byte[]> writeOnStream = writeOnStream(pipedOutputStream);

                // @formatter:off
                exchange.getRequest()
                        .getBody()
                        .cache()
                        .map(DataBuffer::asByteBuffer)
                        .map(getByteArray())
                        .doOnComplete(closeQuietly(pipedOutputStream))
                        .subscribe(writeOnStream);
                // @formatter:on
            }
            executeUpload(bucket, name, contentType, pipedInputStream);
        } catch (IOException e) {
            // TODO: Handle exception
            log.error(e.getMessage(), e);
        }
    }
    private Runnable closeQuietly(OutputStream outputStream) {
        return () -> {
            try {
                outputStream.close();
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        };
    }
    private Flux<byte[]> partToBytes(Part p) {
        return p.content().map(DataBuffer::asByteBuffer).map(getByteArray());
    }
    protected Consumer<byte[]> writeOnStream(PipedOutputStream pipedOutputStream) {
        return byteArray -> {
            try {
                pipedOutputStream.write(byteArray);
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        };
    }
    private void executeUpload(String bucket, String name, String contentType, PipedInputStream pipedInputStream) {
        try {
            while (pipedInputStream.available() == 0)
                Thread.yield();
            while (pipedInputStream.available() > 0)
                log.info(Integer.toString(pipedInputStream.read()));
            pipedInputStream.close();
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
        }
    }
    private Function<ByteBuffer, byte[]> getByteArray() {
        return byteBuffer -> {
            byte[] byteArray = new byte[byteBuffer.remaining()];
            byteBuffer.get(byteArray);
            return byteArray;
        };
    }
    private final Predicate<ServerHttpRequest> isAUpload =
            request -> request.getURI().getPath().contains(OBJECT_STORAGE)
                    && Objects.equals(request.getMethod(), HttpMethod.POST);
    private final Function<ServerHttpRequest, Function<String, String>> getQueryParam =
            request -> queryParam -> this.safelyGetQueryParam(request, queryParam);
    private String safelyGetQueryParam(ServerHttpRequest request, String queryParam) {
        return Optional.ofNullable(request.getQueryParams().get(queryParam)).orElse(Collections.emptyList()).stream()
                .findFirst().orElse(null);
    }
}