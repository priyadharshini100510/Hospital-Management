package com.hospital.hms.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles exceptions thrown from @Controller (view-rendering) request handlers.
 * REST (@RestController under /api) exceptions are handled separately by
 * GlobalRestExceptionHandler so JSON callers get JSON error bodies.
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalMvcExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex) {
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        mav.setStatus(org.springframework.http.HttpStatus.NOT_FOUND);
        return mav;
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        try {
            org.springframework.web.servlet.support.RequestContextUtils.getOutputFlashMap(request)
                    .put("errorMessage", ex.getMessage());
        } catch (Exception ignored) {
        }
        return new ModelAndView("redirect:" + (referer != null ? referer : "/"));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ModelAndView handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException ex, HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        try {
            org.springframework.web.servlet.support.RequestContextUtils.getOutputFlashMap(request)
                    .put("errorMessage", "Operation cannot be completed because this record is referenced by other hospital data (e.g. assigned doctors or appointments).");
        } catch (Exception ignored) {
        }
        return new ModelAndView("redirect:" + (referer != null ? referer : "/"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        return new ModelAndView("error/403");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneral(Exception ex, Model model) {
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", ex.getMessage());
        return mav;
    }
}
