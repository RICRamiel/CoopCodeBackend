package org.ricramiel.coopeditbackend.infrastructure.security.oauth.user_info;

public interface OAuth2UserInfo {
    String getId();
    String getName();
    String getEmail();
    String getImageUrl();
}