package us.magicalash.weasel.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import us.magicalash.weasel.representation.AbstractResponse;
import us.magicalash.weasel.representation.ApiMetadata;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;

@ControllerAdvice
@RestControllerAdvice
public class WebControllerAdvice extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof AbstractResponse) {
            AbstractResponse responseBody = (AbstractResponse) body;
            ApiMetadata meta = responseBody.getMetadata();
            meta.setTimestamp(Instant.now().toString());
            meta.setEndpoint(request.getURI().getPath());

            if (meta.getResponseCode() >= 200 && meta.getResponseCode() < 300) {
                meta.setStatus("success");
            } else if (meta.getResponseCode() >= 400 && meta.getResponseCode() < 500) {
                meta.setStatus("client_error");
            } else if (meta.getResponseCode() >= 500) {
                meta.setStatus("server_error");
            }

            response.setStatusCode(HttpStatus.resolve(meta.getResponseCode()));
        }

        return body;
    }

    @ExceptionHandler
    public ResponseEntity<AbstractResponse> onError(Exception e, HttpServletRequest request, HttpServletResponse response) {
        AbstractResponse responseBody = new AbstractResponse();
        ApiMetadata metadata = responseBody.getMetadata();

        // an exception got thrown, we're probably at fault here
        metadata.setResponseCode(500);
        metadata.setStatus("server_error");

        metadata.setMessage(e.getMessage());

        metadata.setEndpoint(request.getContextPath());
        metadata.setTimestamp(Instant.now().toString());

        response.setStatus(500);
        System.out.println("caught an error: " + e);

        return ResponseEntity.status(500)
                             .body(responseBody);
    }

}
