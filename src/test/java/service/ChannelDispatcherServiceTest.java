package service;

import org.Notification.channel.EmailChannel;
import org.Notification.channel.InAppChannel;
import org.Notification.channel.NotificationChannel;
import org.Notification.channel.PushChannel;
import org.Notification.channel.SmsChannel;
import org.Notification.model.Notification;
import org.Notification.service.ChannelDispatcherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChannelDispatcherServiceTest {

    @Mock EmailChannel email;
    @Mock PushChannel push;
    @Mock InAppChannel inApp;
    @Mock SmsChannel sms;

    private ChannelDispatcherService dispatcher;

    @BeforeEach
    void setUp() {
        when(email.getChannelName()).thenReturn("EMAIL");
        when(push.getChannelName()).thenReturn("PUSH");
        when(inApp.getChannelName()).thenReturn("IN_APP");
        when(sms.getChannelName()).thenReturn("SMS");

        dispatcher = new ChannelDispatcherService(List.of(email, push, inApp, sms));
    }

    @Test
    void dispatch_shouldCallEmailChannel() {
        Notification n = notificationWith("EMAIL");

        dispatcher.dispatch(n);

        verify(email, times(1)).send(n);
        verify(push, never()).send(any());
        verify(inApp, never()).send(any());
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_shouldCallPushChannel() {
        Notification n = notificationWith("PUSH");

        dispatcher.dispatch(n);

        verify(push, times(1)).send(n);
        verify(email, never()).send(any());
        verify(inApp, never()).send(any());
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_shouldCallInAppChannel() {
        Notification n = notificationWith("IN_APP");

        dispatcher.dispatch(n);

        verify(inApp, times(1)).send(n);
        verify(email, never()).send(any());
        verify(push, never()).send(any());
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_shouldCallSmsChannel() {
        Notification n = notificationWith("SMS");

        dispatcher.dispatch(n);

        verify(sms, times(1)).send(n);
        verify(email, never()).send(any());
        verify(push, never()).send(any());
        verify(inApp, never()).send(any());
    }

    @Test
    void dispatch_shouldDoNothingForNullNotification() {
        assertDoesNotThrow(() -> dispatcher.dispatch(null));

        verify(email, never()).send(any());
        verify(push, never()).send(any());
        verify(inApp, never()).send(any());
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_shouldDoNothingForNullChannel() {
        Notification n = new Notification();
        n.setChannel(null);

        assertDoesNotThrow(() -> dispatcher.dispatch(n));

        verify(email, never()).send(any());
        verify(push, never()).send(any());
        verify(inApp, never()).send(any());
        verify(sms, never()).send(any());
    }

    @Test
    void dispatch_shouldMarkFailedAfterMaxRetries() {
        Notification n = notificationWith("EMAIL");
        n.setRetryCount(3);

        doThrow(new RuntimeException("fail")).when(email).send(n);

        assertDoesNotThrow(() -> dispatcher.dispatch(n));

        assertEquals("FAILED", n.getStatus());
    }

    private Notification notificationWith(String channel) {
        Notification n = new Notification();
        n.setUserId("user1");
        n.setTitle("Test");
        n.setDescription("Test desc");
        n.setChannel(channel);
        n.setRetryCount(3);
        return n;
    }
}