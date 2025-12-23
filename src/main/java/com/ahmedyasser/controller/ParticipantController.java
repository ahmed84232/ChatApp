//package com.ahmedyasser.controller;
//
//import com.ahmedyasser.service.ConversationService;
//import com.ahmedyasser.service.ParticipantService;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/participant")
//public class ParticipantController {
//
//    private final ParticipantService participantService;
//
//    public ParticipantController(ParticipantService participantService) {
//        this.participantService = participantService;
//    }
//
//    @PostMapping
//    public ResponseEntity<?> addParticipant(@RequestBody Map<String, UUID> body) {
//        participantService.addParticipant(
//                body.get("conversationId"),
//                body.get("userId")
//        );
//
//        return ResponseEntity.ok("Participant added");
//    }
//
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<?> deleteParticipant(@PathVariable UUID userId, @RequestParam UUID conversationId) {
//        participantService.deleteParticipant(conversationId, userId);
//        return ResponseEntity.noContent().build();
//    }
//
//}
