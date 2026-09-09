package com.knowledge.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.knowledge.api.common.Result;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class CommonWebInfrastructureTest {

    @Test
    void businessErrorUsesHttpStatusAndUnifiedResult() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/orders/missing");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-001");

        ResponseEntity<Result<Void>> response = new GlobalExceptionHandler().handleBusiness(
                BusinessException.notFound("订单不存在"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("订单不存在");
        assertThat(response.getBody().requestId()).isEqualTo("request-001");
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void requestIdFilterPropagatesSafeClientValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "web-20260903:001");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("web-20260903:001");
        assertThat(request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)).isEqualTo("web-20260903:001");
    }

    @Test
    void requestIdFilterReplacesUnsafeClientValue() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "unsafe request id");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RequestIdFilter().doFilter(request, response, new MockFilterChain());

        String generated = response.getHeader(RequestIdFilter.HEADER);
        assertThat(generated).isNotEqualTo("unsafe request id");
        assertThat(UUID.fromString(generated)).isNotNull();
    }

    @Test
    void methodAuthorizationDeniedUsesForbiddenResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/admin/courses");
        request.setAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE, "request-forbidden");

        ResponseEntity<Result<Void>> response = new GlobalExceptionHandler()
                .handleAccessDenied(new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo(403);
        assertThat(response.getBody().requestId()).isEqualTo("request-forbidden");
    }
}
