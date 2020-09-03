package com.gojek.esb.sink.redis.client;

import com.gojek.esb.consumer.EsbMessage;
import com.gojek.esb.exception.DeserializerException;
import com.gojek.esb.sink.redis.dataentry.RedisDataEntry;
import com.gojek.esb.sink.redis.dataentry.RedisHashSetFieldEntry;
import com.gojek.esb.sink.redis.dataentry.RedisListEntry;
import com.gojek.esb.sink.redis.exception.NoResponseException;
import com.gojek.esb.sink.redis.parsers.RedisParser;
import com.gojek.esb.sink.redis.ttl.RedisTTL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RedisStandaloneClientTest {
    private final RedisHashSetFieldEntry firstRedisSetEntry = new RedisHashSetFieldEntry("key1", "field1", "value1");
    private final RedisHashSetFieldEntry secondRedisSetEntry = new RedisHashSetFieldEntry("key2", "field2", "value2");
    private final RedisListEntry firstRedisListEntry = new RedisListEntry("key1", "value1");
    private final RedisListEntry secondRedisListEntry = new RedisListEntry("key2", "value2");
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private RedisClient redisClient;
    private List<EsbMessage> esbMessages;
    private List<RedisDataEntry> redisDataEntries;
    @Mock
    private RedisParser redisMessageParser;

    @Mock
    private RedisTTL redisTTL;

    @Mock
    private Jedis jedis;

    @Mock
    private Pipeline jedisPipeline;

    @Mock
    private Response<List<Object>> responses;


    @Before
    public void setUp() {
        esbMessages = Arrays.asList(new EsbMessage(new byte[0], new byte[0], "topic", 0, 100),
                new EsbMessage(new byte[0], new byte[0], "topic", 0, 100));

        redisClient = new RedisStandaloneClient(redisMessageParser, redisTTL, jedis);

        redisDataEntries = new ArrayList<>();

        when(jedis.pipelined()).thenReturn(jedisPipeline);
        when(redisMessageParser.parse(esbMessages)).thenReturn(redisDataEntries);
    }

    @Test
    public void pushesDataEntryForListInATransaction() throws DeserializerException {
        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);

        redisClient.prepare(esbMessages);

        verify(jedisPipeline, times(1)).multi();
        verify(jedisPipeline).lpush(firstRedisListEntry.getKey(), firstRedisListEntry.getValue());
        verify(jedisPipeline).lpush(secondRedisListEntry.getKey(), secondRedisListEntry.getValue());
    }

    @Test
    public void setsTTLForListItemsInATransaction() throws DeserializerException {
        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);

        redisClient.prepare(esbMessages);

        verify(redisTTL).setTTL(jedisPipeline, firstRedisListEntry.getKey());
        verify(redisTTL).setTTL(jedisPipeline, secondRedisListEntry.getKey());
    }

    @Test
    public void pushesDataEntryForSetInATransaction() throws DeserializerException {
        populateRedisDataEntry(firstRedisSetEntry, secondRedisSetEntry);

        redisClient.prepare(esbMessages);

        verify(jedisPipeline, times(1)).multi();
        verify(jedisPipeline).hset(firstRedisSetEntry.getKey(), firstRedisSetEntry.getField(), firstRedisSetEntry.getValue());
        verify(jedisPipeline).hset(secondRedisSetEntry.getKey(), secondRedisSetEntry.getField(), secondRedisSetEntry.getValue());
    }

    @Test
    public void setsTTLForSetItemsInATransaction() throws DeserializerException {
        populateRedisDataEntry(firstRedisSetEntry, secondRedisSetEntry);

        redisClient.prepare(esbMessages);

        verify(redisTTL).setTTL(jedisPipeline, firstRedisSetEntry.getKey());
        verify(redisTTL).setTTL(jedisPipeline, secondRedisSetEntry.getKey());
    }

    @Test
    public void shouldCompleteTransactionInExec() {
        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);
        when(jedisPipeline.exec()).thenReturn(responses);
        when(responses.get()).thenReturn(Collections.singletonList("MOCK_LIST_ITEM"));

        redisClient.prepare(esbMessages);
        redisClient.execute();

        verify(jedisPipeline).exec();
    }

    @Test
    public void shouldWaitForResponseInExec() {
        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);
        when(jedisPipeline.exec()).thenReturn(responses);
        when(responses.get()).thenReturn(Collections.singletonList("MOCK_LIST_ITEM"));

        redisClient.prepare(esbMessages);
        redisClient.execute();

        verify(jedisPipeline).sync();
    }

    @Test
    public void shouldThrowExceptionWhenResponseIsNullInExec() {
        expectedException.expect(NoResponseException.class);

        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);
        when(jedisPipeline.exec()).thenReturn(responses);
        when(responses.get()).thenReturn(null);

        redisClient.prepare(esbMessages);
        redisClient.execute();
    }

    @Test
    public void shouldThrowExceptionWhenResponseIsEmptyInExec() {
        expectedException.expect(NoResponseException.class);

        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);
        when(jedisPipeline.exec()).thenReturn(responses);
        when(responses.get()).thenReturn(new ArrayList<>());

        redisClient.prepare(esbMessages);
        redisClient.execute();
    }

    @Test
    public void shouldReturnEmptyArrayInExec() {
        populateRedisDataEntry(firstRedisListEntry, secondRedisListEntry);
        when(jedisPipeline.exec()).thenReturn(responses);
        when(responses.get()).thenReturn(Collections.singletonList("MOCK_LIST_ITEM"));

        redisClient.prepare(esbMessages);
        List<EsbMessage> elementsToRetry = redisClient.execute();

        Assert.assertEquals(0, elementsToRetry.size());
    }

    @Test
    public void shouldCloseTheClient() {
        redisClient.close();

        verify(jedis, times(1)).close();
    }


    private void populateRedisDataEntry(RedisDataEntry... redisData) {
        redisDataEntries.addAll(Arrays.asList(redisData));
    }
}