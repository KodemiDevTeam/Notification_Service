package org.notification.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionClassesTest {

    @Test
    void testExceptionConstructors() {
        Throwable cause = new RuntimeException("root cause");

        DownstreamServiceException dse1 = new DownstreamServiceException("msg");
        DownstreamServiceException dse2 = new DownstreamServiceException("msg", cause);
        assertEquals("msg", dse1.getMessage());
        assertEquals("msg", dse2.getMessage());
        assertEquals(cause, dse2.getCause());

        EmailSendingException ese2 = new EmailSendingException("msg", cause);
        assertEquals("msg", ese2.getMessage());
        assertEquals(cause, ese2.getCause());

        InAppNotificationException iae2 = new InAppNotificationException("msg", cause);
        assertEquals("msg", iae2.getMessage());
        assertEquals(cause, iae2.getCause());

        InvalidAuthorizationException iauth = new InvalidAuthorizationException("msg");
        assertEquals("msg", iauth.getMessage());

        InvalidDeviceTokenException idt1 = new InvalidDeviceTokenException("msg");
        InvalidDeviceTokenException idt2 = new InvalidDeviceTokenException("msg", cause);
        assertEquals("msg", idt1.getMessage());
        assertEquals(cause, idt2.getCause());

        InvalidEmailException ie1 = new InvalidEmailException("msg");
        InvalidEmailException ie2 = new InvalidEmailException("msg", cause);
        assertEquals("msg", ie1.getMessage());
        assertEquals(cause, ie2.getCause());

        InvalidJwtException ij1 = new InvalidJwtException("msg");
        InvalidJwtException ij2 = new InvalidJwtException("msg", cause);
        assertEquals("msg", ij1.getMessage());
        assertEquals("msg", ij2.getMessage());
        assertEquals(cause, ij2.getCause());

        InvalidMessageException ime = new InvalidMessageException("msg");
        assertEquals("msg", ime.getMessage());

        InvalidPhoneNumberException ipne = new InvalidPhoneNumberException("msg");
        assertEquals("msg", ipne.getMessage());

        InvalidUserIdException iue1 = new InvalidUserIdException("msg");
        InvalidUserIdException iue2 = new InvalidUserIdException("msg", cause);
        assertEquals("msg", iue1.getMessage());
        assertEquals(cause, iue2.getCause());


        NotificationNotFoundException nnfe = new NotificationNotFoundException("msg");
        assertEquals("msg", nnfe.getMessage());

        NotificationValidationException nve = new NotificationValidationException("msg");
        assertEquals("msg", nve.getMessage());

        PermanentFailureException pfe1 = new PermanentFailureException("msg");
        PermanentFailureException pfe2 = new PermanentFailureException("msg", cause);
        assertEquals("msg", pfe1.getMessage());
        assertEquals("msg", pfe2.getMessage());
        assertEquals(cause, pfe2.getCause());

        ProviderDisabledException pde = new ProviderDisabledException("msg");
        assertEquals("msg", pde.getMessage());

        PushSendingException pse2 = new PushSendingException("msg", cause);
        assertEquals("msg", pse2.getMessage());
        assertEquals(cause, pse2.getCause());

        SmsSendingException sse2 = new SmsSendingException("msg", cause);
        assertEquals("msg", sse2.getMessage());
        assertEquals(cause, sse2.getCause());

    }
}

