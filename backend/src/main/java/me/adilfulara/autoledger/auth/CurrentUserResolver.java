package me.adilfulara.autoledger.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentUser} annotation in controller method parameters.
 *
 * <p>Extracts {@link AuthenticatedUser} from request attribute set by {@link JwtAuthFilter}.
 */
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
            && parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("HttpServletRequest is null");
        }

        AuthenticatedUser user = (AuthenticatedUser) request.getAttribute(JwtAuthFilter.AUTHENTICATED_USER_ATTRIBUTE);
        if (user == null) {
            throw new IllegalStateException("No authenticated user found in request");
        }

        return user;
    }
}
