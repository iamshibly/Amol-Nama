package com.safayet.afsos_nama;

import com.safayet.afsos_nama.config.ErrorDiagnostics;
import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorDiagnosticsTest {
    @Test
    void rejectedRequestGetsAReferenceWithoutAnException() {
        var request = new MockHttpServletRequest("POST", "/profile");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);
        var view = new ModelAndView("error");
        new ErrorDiagnostics().postHandle(request, new MockHttpServletResponse(), new Object(), view);
        assertEquals(400, view.getModel().get("diagnosticStatus"));
        assertEquals("REQUEST_REJECTED", view.getModel().get("diagnosticCode"));
        assertTrue(view.getModel().get("errorReference").toString().matches("[a-f0-9]{8}"));
    }

    @Test
    void databaseDetailsStayOutOfTheRenderedDiagnostic() {
        var request = new MockHttpServletRequest("GET", "/error");
        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 500);
        request.setAttribute(RequestDispatcher.ERROR_EXCEPTION,
                new RuntimeException("private value", new SQLException("private SQL", "23000", 1048)));
        var view = new ModelAndView("error");
        new ErrorDiagnostics().postHandle(request, new MockHttpServletResponse(), new Object(), view);
        assertEquals("DATABASE_23000_1048", view.getModel().get("diagnosticCode"));
        assertFalse(view.getModel().toString().contains("private"));
    }
}
