package models;

import lombok.Data;

@Data
public class FlyingPigsApiAuthResult {
    private String accessToken;

    private Authentication authentication;

    private User user;

    public String getAccessToken() {
        return accessToken;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public User getUser() {
        return user;
    }

    public static class Authentication {
        private String strategy;

        private String accessToken;

        private Payload payload;

        public String getStrategy() {
            return strategy;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public Payload getPayload() {
            return payload;
        }
    }

    public static class Payload {
        private long iat;

        private long exp;

        private String aud;

        private String iss;
        private String sub;

        private String jti;

        public long getIat() {
            return iat;
        }

        public long getExp() {
            return exp;
        }

        public String getAud() {
            return aud;
        }

        public String getIss() {
            return iss;
        }

        public String getSub() {
            return sub;
        }

        public String getJti() {
            return jti;
        }
    }

    public static class User {
        private String id;

        private String email;

        private String createdAt;

        private String updatedAt;

        private String deletedAt;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public String getDeletedAt() {
            return deletedAt;
        }
    }
}
