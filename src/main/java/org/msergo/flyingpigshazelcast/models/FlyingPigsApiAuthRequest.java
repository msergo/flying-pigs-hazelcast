package org.msergo.flyingpigshazelcast.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

@Data
public class FlyingPigsApiAuthRequest {
    private String email;
    private String password;
    private final String strategy = "local";

    public FlyingPigsApiAuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String toJsonString() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }
}
