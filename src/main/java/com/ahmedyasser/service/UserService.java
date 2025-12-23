package com.ahmedyasser.service;

import com.ahmedyasser.dao.UserDao;
import com.ahmedyasser.dto.UserDto;
import com.ahmedyasser.entity.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class UserService {

    private final UserDao userDao;
    @Value("${FRONTEND_HOST}")
    private String frontendHost;

    @Value("${KC_HOST}")
    private String keycloakHost;

    @Value("${KC_REALM_NAME}")
    private String keycloakRealm;

    @Value("${KC_CLIENT_ID}")
    private String clientId;

    @Value("${KC_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${KC_INTERNAL_CLIENT_ID}")
    private String internalClientId;

    @Value("${KC_INTERNAL_CLIENT_SECRET}")
    private String internalClientSecret;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }


    public UserDto findUserById(UUID id) {

        ObjectMapper mapper = new ObjectMapper();

        MultiValueMap<String, Object> keycloakBody = new LinkedMultiValueMap<>();
        keycloakBody.add("client_id", internalClientId);
        keycloakBody.add("client_secret", internalClientSecret);
        keycloakBody.add("grant_type", "client_credentials");

        RestClient restClient = RestClient
                .builder()
                .baseUrl(keycloakHost)
                .build();

        String response;

        try {
            response = restClient.post()
                    .uri("/realms/%s/protocol/openid-connect/token".formatted(keycloakRealm))
                    .body(keycloakBody)
                    .retrieve()
                    .body(String.class);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getResponseBodyAsString());
        }

        HashMap<String, String> token;
        try {
            token = mapper.readValue(response, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/{realm}/users/{id}")
                        .build(keycloakRealm, id.toString())
                ).header("Authorization", "Bearer " + token.get("access_token"))
                .retrieve()
                .body(String.class);

        Map<String, Object> userMap;
        try {
            userMap = mapper.readValue(result, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        UserDto user = new UserDto();

        user.setId(UUID.fromString((String) userMap.get("id")));
        user.setUsername((String) userMap.get("username"));

        return user;
    }

    @Transactional
    public List<UserDto> findUserByUsername(String query) {

        ObjectMapper mapper = new ObjectMapper();

        MultiValueMap<String, Object> keycloakBody = new LinkedMultiValueMap<>();
        keycloakBody.add("client_id", internalClientId);
        keycloakBody.add("client_secret", internalClientSecret);
        keycloakBody.add("grant_type", "client_credentials");

        RestClient restClient = RestClient
                .builder()
                .baseUrl(keycloakHost)
                .build();

        String response;

        try {
            response = restClient.post()
                    .uri("/realms/%s/protocol/openid-connect/token".formatted(keycloakRealm))
                    .body(keycloakBody)
                    .retrieve()
                    .body(String.class);

        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getResponseBodyAsString());
        }

        HashMap<String, String> token;
        try {
            token = mapper.readValue(response, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/admin/realms/%s/users".formatted(keycloakRealm))
                        .queryParam("username", query)
                        .build()
                ).header("Authorization", "Bearer " + token.get("access_token"))
                .retrieve()
                .body(String.class);

        List<Map<String, Object>> users;
        try {
            users = mapper.readValue(result, new TypeReference<>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return users.stream()
                .map(x -> {
                    UserDto user = new UserDto();
                    user.setId(UUID.fromString((String) x.get("id")));
                    user.setUsername((String) x.get("username"));
                    return user;
                })
                .filter(u ->!u.getUsername().startsWith("service-account-"))
                .toList();
    }

    public ResponseEntity<String> getToken(String body){

        // Exchange code for access token
        ObjectMapper mapper = new ObjectMapper();
        HashMap<String, String> map;
        try {
            map = mapper.readValue(body, HashMap.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        MultiValueMap<String, Object> keycloakBody = new LinkedMultiValueMap<>();

        keycloakBody.add("code", map.get("code"));
        keycloakBody.add("redirect_uri", "%s/auth/callback".formatted(frontendHost));
        keycloakBody.add("client_id", clientId);
        keycloakBody.add("client_secret", clientSecret);
        keycloakBody.add("grant_type", "authorization_code");

        RestClient restClient = RestClient.builder().build();
        String response;

        try {
            response = restClient.post()
                    .uri("%s/realms/%s/protocol/openid-connect/token".formatted(keycloakHost, keycloakRealm))
                    .body(keycloakBody)
                    .retrieve()
                    .body(String.class);

        } catch (HttpClientErrorException e) {
            return ResponseEntity
                    .status(e.getStatusCode())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getResponseBodyAsString());

        }
        return ResponseEntity.ok(response);
    }

    public UserDto saveUser(User user){
        User savedUser = userDao.save(user);
        return UserDto.toDto(savedUser);
    }

    public UserDto findDbUserById(UUID id){
        Optional<User> user = userDao.findById(id);
        return user.map(UserDto::toDto).orElse(null);

    }

    public List<UserDto> findAllUsernamesByUserIds(List<UUID> userIds){
        List<User> users = userDao.findAllById(userIds);
        return users.stream().map(UserDto::toDto).toList();
    }

}
