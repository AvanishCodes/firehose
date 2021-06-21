package io.odpf.firehose.sink.objectstorage.message;

import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Int64Value;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.StringValue;
import com.gojek.de.stencil.parser.Parser;

import io.odpf.firehose.consumer.Message;
import io.odpf.firehose.exception.DeserializerException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;
import static junit.framework.TestCase.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class MessageDeSerializerTest {

    @Mock
    private KafkaMetadataUtils kafkaMetadataUtils;

    @Mock
    private Parser protoParser;

    private MessageDeSerializer deSerializer;

    private final byte[] logKey = "key".getBytes();
    private final byte[] logMessage = "msg".getBytes();
    private Message message;

    @Before
    public void setUp() throws Exception {
        message = new Message(logKey, logMessage, "topic1", 0, 100);
        deSerializer = new MessageDeSerializer(kafkaMetadataUtils, true, protoParser);
    }

    @Test
    public void shouldCreateRecord() throws InvalidProtocolBufferException {

        DynamicMessage dynamicMessage = DynamicMessage.newBuilder(StringValue.of("abc")).build();
        DynamicMessage metadataMessage = DynamicMessage.newBuilder(Int64Value.of(112)).build();

        when(protoParser.parse(logMessage)).thenReturn(dynamicMessage);
        when(kafkaMetadataUtils.createKafkaMetadata(message)).thenReturn(metadataMessage);

        Record record = deSerializer.deSerialize(message);

        assertEquals(record.getMetadata(), metadataMessage);
        assertEquals(record.getMessage(), dynamicMessage);

        verify(protoParser, times(1)).parse(logMessage);
        verify(kafkaMetadataUtils, times(1)).createKafkaMetadata(message);
    }

    @Test(expected = DeserializerException.class)
    public void shouldThrowDeserializerExceptionWhenProtoParsingThrowException() throws InvalidProtocolBufferException {
        InvalidProtocolBufferException invalidProtocolBufferException = new InvalidProtocolBufferException("");
        when(protoParser.parse(logMessage)).thenThrow(invalidProtocolBufferException);

        Record record = deSerializer.deSerialize(message);
    }
}