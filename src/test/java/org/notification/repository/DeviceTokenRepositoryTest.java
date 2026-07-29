package org.notification.repository;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.notification.model.DeviceToken;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeviceTokenRepositoryTest {

    private DynamoDBMapper mapper;
    private DeviceTokenRepository repository;

    @BeforeEach
    void setUp() {
        mapper = mock(DynamoDBMapper.class);
        repository = new DeviceTokenRepository(mapper);
    }

    @Test
    void testSave() {
        DeviceToken token = new DeviceToken();
        assertDoesNotThrow(() -> repository.save(token));
        verify(mapper).save(token);
    }

    @Test
    void testDelete() {
        DeviceToken token = new DeviceToken();
        assertDoesNotThrow(() -> repository.delete(token));
        verify(mapper).delete(token);
    }

    @Test
    void testFindByToken() {
        DeviceToken token = new DeviceToken();
        when(mapper.load(DeviceToken.class, "token1")).thenReturn(token);

        DeviceToken res = repository.findByToken("token1");
        assertEquals(token, res);
    }

    @Test
    void testFindByUserId() {
        when(mapper.query(eq(DeviceToken.class), any())).thenReturn(null);

        List<DeviceToken> res = repository.findByUserId("u1");
        assertNull(res);
        verify(mapper).query(eq(DeviceToken.class), any());
    }
}
