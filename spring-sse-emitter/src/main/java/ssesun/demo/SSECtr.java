package ssesun.demo;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RequestMapping("test")
@RestController()
public class SSECtr {

    public static List<SseEmitter> connections = new CopyOnWriteArrayList<>();

    @GetMapping()
    public SseEmitter registerSSE() throws IOException {
        SseEmitter sseEmitter = new SseEmitter(-1l);
        connections.add(sseEmitter);
        sendConnections();
        return sseEmitter;
    }

    @Scheduled(fixedRate = 1000)
    public void hearbeat() throws IOException {
        Iterator<SseEmitter> iterator = connections.iterator();
        while(iterator.hasNext()) {
            SseEmitter next = iterator.next();
            try {
                next.send(SseEmitter.event().comment("hb").build());
            } catch (Exception e) {
                e.printStackTrace();
                connections.remove(next);
                sendConnections();
            }
        }
    }

    private void sendConnections() throws IOException {
        Iterator<SseEmitter> iterator = connections.iterator();
        while(iterator.hasNext()) {
            SseEmitter next = iterator.next();
            try {
                next.send(SseEmitter.event().data(connections.size()).build());
            } catch (Exception e) {
                e.printStackTrace();
                connections.remove(next);
                sendConnections();
            }
        }
    }
}
