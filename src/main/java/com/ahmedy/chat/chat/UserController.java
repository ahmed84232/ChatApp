package com.ahmedy.chat.chat;

import com.ahmedy.chat.dto.UserDto;
import com.ahmedy.chat.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class UserController {

    private final UserService userService;

    @Value("${FRONTNED_HOST}")
    private String frontendHost;

    @Value("${KC_HOST}")
    private String keycloakHost;

    @Value("${KC_REALM_NAME}")
    private String keycloakRealm;

    @Value("${KC_CLIENT_ID}")
    private String clientId;

    @Value("${KC_CLIENT_SECRET}")
    private String clientSecret;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/token")
    public ResponseEntity<String> getToken(@RequestBody String body) throws JsonProcessingException {

        // Exchange code for access token
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> map = mapper.readValue(body, HashMap.class);

        MultiValueMap<String, Object> keycloakBody = new LinkedMultiValueMap<>();

        keycloakBody.add("code", map.get("code"));
        keycloakBody.add("redirect_uri", "%s/auth/callback".formatted(frontendHost));
        keycloakBody.add("client_id", clientId);
        keycloakBody.add("client_secret", clientSecret);
        keycloakBody.add("grant_type", "authorization_code");


        RestClient restClient = RestClient.builder().build();
        String response = "";

        try {
            response = restClient.post()
                    .uri("%s/realms/%s/protocol/openid-connect/token".formatted(keycloakHost, keycloakRealm))
                    .body(keycloakBody)
                    .retrieve()
                    .body(String.class);

        } catch (HttpClientErrorException e) {
            System.out.println(e.getResponseBodyAsString());

            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());

        };
        System.out.println(response);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/users")
    public ResponseEntity<UserDto> addUser(@RequestBody UserDto request) {
        return ResponseEntity.ok(userService.addUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody UserDto request) {

        return ResponseEntity.ok(userService.findUserByUsername(request.getUsername()));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<UserDto> getUser(@PathVariable String username) {
        return ResponseEntity.ok(userService.findUserByUsername(username));
    }

    @GetMapping()
    public ResponseEntity<?> getUsers(@RequestParam String username) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
//        HashMap<String, String> map = mapper.readValue(body, HashMap.class);

        MultiValueMap<String, Object> keycloakBody = new LinkedMultiValueMap<>();
        keycloakBody.add("client_id", "ChatAppInternal");
        keycloakBody.add("client_secret", "eUYaHz4vF5r1fWNnAQ3vhEkE2oMprZpO");
        keycloakBody.add("grant_type", "client_credentials");

        RestClient restClient = RestClient
                .builder()
                .baseUrl(keycloakHost)
                .build();

        String response = "";

        try {
            response = restClient.post()
                    .uri("/realms/%s/protocol/openid-connect/token".formatted(keycloakRealm))
                    .body(keycloakBody)
                    .retrieve()
                    .body(String.class);

        } catch (HttpClientErrorException e) {
            System.out.println(e.getResponseBodyAsString());

            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());

        };
        System.out.println(response);
        HashMap<String, String> token = mapper.readValue(response, new TypeReference<>(){});
        System.out.println(token.get("access_token"));

        String result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/%s/users".formatted(keycloakRealm))
                        .queryParam("username", username)
                        .build()
                ).header("Authorization", "Bearer " + token.get("access_token"))
                .retrieve()
                .body(String.class);

        HashMap<String, String> user = mapper.readValue(result, new TypeReference<>(){});
        return ResponseEntity.ok(user.get("id"));
    }

}
