package com.example.userapi.security;

import com.example.userapi.entity.User;
import com.example.userapi.enums.AuthProvider;
import com.example.userapi.enums.UserStatus;
import com.example.userapi.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public OAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerId = extractProviderId(provider, attributes);
        String email = extractEmail(provider, attributes);
        String name = extractName(provider, attributes);

        Optional<User> existingUser = userRepository.findByProviderAndProviderId(provider, providerId);

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setProvider(provider);
            user.setProviderId(providerId);
            user.setEmail(email != null ? email : provider.name().toLowerCase() + "_" + providerId + "@oauth.local");
            user.setUsername(generateUsername(provider, providerId, email));
            user.setFirstName(name);
            user.setStatus(UserStatus.ENABLED);
            user = userRepository.save(user);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }

    private String extractProviderId(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("sub");
            case GITHUB -> String.valueOf(attributes.get("id"));
            case LINE -> (String) attributes.get("userId");
            default -> throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        };
    }

    private String extractEmail(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE, GITHUB -> (String) attributes.get("email");
            case LINE -> (String) attributes.get("email");
            default -> null;
        };
    }

    private String extractName(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> (String) attributes.get("name");
            case GITHUB -> (String) attributes.getOrDefault("name", attributes.get("login"));
            case LINE -> (String) attributes.get("displayName");
            default -> null;
        };
    }

    private String generateUsername(AuthProvider provider, String providerId, String email) {
        if (email != null && !userRepository.existsByUsername(email.split("@")[0])) {
            return email.split("@")[0];
        }
        return provider.name().toLowerCase() + "_" + providerId;
    }
}
