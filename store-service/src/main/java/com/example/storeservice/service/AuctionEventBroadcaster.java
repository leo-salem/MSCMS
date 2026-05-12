package com.example.storeservice.service;

import com.example.storeservice.dto.response.AuctionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory SSE broadcaster keyed by auctionId. Single-instance only.
 * For multi-instance deployments, replace with Redis pub/sub or Kafka fan-out.
 */
@Component
@Slf4j
public class AuctionEventBroadcaster {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    @Value("${mscms.store.sse-heartbeat-seconds:25}")
    private long heartbeatSeconds;

    public SseEmitter subscribe(Long auctionId) {
        SseEmitter emitter = new SseEmitter(0L); // never time out
        emitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(auctionId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) emitters.remove(auctionId);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(AuctionEvent.builder()
                            .type("connected")
                            .auctionId(auctionId)
                            .timestamp(LocalDateTime.now())
                            .build()));
        } catch (IOException ignored) {}
        return emitter;
    }

    public void publish(Long auctionId, AuctionEvent event) {
        List<SseEmitter> list = emitters.get(auctionId);
        if (list == null || list.isEmpty()) return;
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event.getType()).data(event));
            } catch (Exception e) {
                emitter.complete();
            }
        }
    }

    /** Periodic heartbeat so proxies don't drop idle SSE connections. */
    @Scheduled(fixedDelayString = "${mscms.store.sse-heartbeat-seconds:25}000")
    public void heartbeat() {
        if (emitters.isEmpty()) return;
        AuctionEvent hb = AuctionEvent.builder().type("heartbeat").timestamp(LocalDateTime.now()).build();
        emitters.forEach((auctionId, list) -> {
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data(hb));
                } catch (Exception e) {
                    emitter.complete();
                }
            }
        });
    }
}
