package org.notification.repository;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.QueryResultPage;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.dynamodbv2.model.UpdateItemRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.notification.model.Notification;
import org.notification.model.enums.NotificationStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @Mock
    private AmazonDynamoDB amazonDynamoDB;

    @InjectMocks
    private NotificationRepository repository;

    @Test
    void testSave() {
        Notification n = new Notification();
        repository.save(n);
        verify(dynamoDBMapper, times(1)).save(n);
    }

    @Test
    void testFindById() {
        Notification n = new Notification();
        when(dynamoDBMapper.load(Notification.class, "id")).thenReturn(n);
        
        Notification result = repository.findById("id");
        assertNotNull(result);
    }

    @Test
    void testFindDueNotifications() {
        QueryResultPage<Notification> page = new QueryResultPage<>();
        page.setResults(List.of(new Notification()));

        when(dynamoDBMapper.queryPage(eq(Notification.class), any(DynamoDBQueryExpression.class))).thenReturn(page);

        List<Notification> results = repository.findDueNotifications(System.currentTimeMillis(), 50, NotificationStatus.PENDING);
        assertEquals(1, results.size());
    }

    @Test
    void testAcquireLock_Success() {
        when(amazonDynamoDB.updateItem(any(UpdateItemRequest.class))).thenReturn(null);
        
        boolean locked = repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING);
        
        assertTrue(locked);
        verify(amazonDynamoDB, times(1)).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    void testAcquireLock_Failure() {
        when(amazonDynamoDB.updateItem(any(UpdateItemRequest.class))).thenThrow(new ConditionalCheckFailedException("Mismatch"));
        
        boolean locked = repository.acquireLock("n1", NotificationStatus.PENDING, NotificationStatus.PROCESSING);
        
        assertFalse(locked);
    }

    @Test
    void testSaveIdempotent_Success() {
        Notification n = new Notification();
        doNothing().when(dynamoDBMapper).save(eq(n), any(com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression.class));

        boolean result = repository.saveIdempotent(n);
        assertTrue(result);
    }

    @Test
    void testSaveIdempotent_Failure() {
        Notification n = new Notification();
        doThrow(new ConditionalCheckFailedException("Duplicate"))
                .when(dynamoDBMapper).save(eq(n), any(com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression.class));

        boolean result = repository.saveIdempotent(n);
        assertFalse(result);
    }

    @Test
    void testFindByUserId() {
        com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList<Notification> paginatedList = mock(com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList.class);
        when(dynamoDBMapper.query(eq(Notification.class), any(DynamoDBQueryExpression.class))).thenReturn(paginatedList);

        List<Notification> result = repository.findByUserId("user1");
        assertEquals(paginatedList, result);
    }

    @Test
    void testExistsByRecurrenceKey_True() {
        QueryResultPage<Notification> page = new QueryResultPage<>();
        page.setResults(List.of(new Notification()));
        when(dynamoDBMapper.queryPage(eq(Notification.class), any(DynamoDBQueryExpression.class))).thenReturn(page);

        boolean exists = repository.existsByRecurrenceKey("key1");
        assertTrue(exists);
    }

    @Test
    void testExistsByRecurrenceKey_False() {
        QueryResultPage<Notification> page = new QueryResultPage<>();
        page.setResults(List.of());
        when(dynamoDBMapper.queryPage(eq(Notification.class), any(DynamoDBQueryExpression.class))).thenReturn(page);

        boolean exists = repository.existsByRecurrenceKey("key1");
        assertFalse(exists);
    }

    @Test
    void testFindByBatchId() {
        com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList<Notification> paginatedList = mock(com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList.class);
        when(dynamoDBMapper.query(eq(Notification.class), any(DynamoDBQueryExpression.class))).thenReturn(paginatedList);

        List<Notification> result = repository.findByBatchId("batch1");
        assertEquals(paginatedList, result);
    }

    @Test
    void testDelete() {
        Notification n = new Notification();
        repository.delete(n);
        verify(dynamoDBMapper, times(1)).delete(n);
    }
}

