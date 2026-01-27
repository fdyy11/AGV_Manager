// java
package com.example.controller;

import com.example.common.config.TcpStatusWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private TcpStatusWebSocketHandler tcpStatusWebSocketHandler;

    @GetMapping("/ws-sessions")
    public ResponseEntity<?> getWebSocketSessions() {
        Map<String, Object> resp = new HashMap<>();
        resp.put("sessionCount", tcpStatusWebSocketHandler.getSessionCount());
        resp.put("sessions", tcpStatusWebSocketHandler.getSessionDetails());
        return ResponseEntity.ok(resp);
    }
}
