package com.anshuman.ticketbooking.service;

import com.anshuman.ticketbooking.dto.SeatDTO;
import java.util.Objects;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = Objects.requireNonNull(messagingTemplate, "messagingTemplate");
    }

    public void broadcastSeatUpdate(SeatDTO seat) {
        // We broadcast the authoritative seat state so all UI clients converge quickly.
        messagingTemplate.convertAndSend("/topic/seats", seat);
    }
}


