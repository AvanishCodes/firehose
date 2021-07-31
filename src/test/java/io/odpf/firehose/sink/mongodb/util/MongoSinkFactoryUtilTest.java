package io.odpf.firehose.sink.mongodb.util;

import io.odpf.firehose.config.MongoSinkConfig;
import io.odpf.firehose.metrics.Instrumentation;
import org.aeonbits.owner.ConfigFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class MongoSinkFactoryUtilTest {

    private MongoSinkConfig mongoSinkConfig;

    @Mock
    private Instrumentation instrumentation;


    @Before
    public void setup() {
        initMocks(this);

        HashMap<String, String> config = new HashMap<>();
        config.put("SINK_MONGO_CONNECTION_URLS", "localhost:8080");
        config.put("SINK_MONGO_DB_NAME", "myDb");
        config.put("SINK_MONGO_PRIMARY_KEY", "customer_id");
        config.put("SINK_MONGO_INPUT_MESSAGE_TYPE", "JSON");
        config.put("SINK_MONGO_COLLECTION_NAME", "customers");
        config.put("SINK_MONGO_REQUEST_TIMEOUT_MS", "8277");
        config.put("SINK_MONGO_RETRY_STATUS_CODE_BLACKLIST", "11000");
        config.put("SINK_MONGO_MODE_UPDATE_ONLY_ENABLE", "true");

        mongoSinkConfig = ConfigFactory.create(MongoSinkConfig.class, config);
    }


    @Test
    public void shouldLogMongoSinkConfig() {

        MongoSinkFactoryUtil.logMongoConfig(mongoSinkConfig, instrumentation);
        verify(instrumentation, times(1)).logDebug(any());

    }

    @Test
    public void shouldLogMongoSinkConfigWithCorrectMessage() {

        MongoSinkFactoryUtil.logMongoConfig(mongoSinkConfig, instrumentation);
        verify(instrumentation, times(1)).logDebug("\n\tMONGO connection urls: localhost:8080\n\tMONGO DB name: myDb\n\tMONGO Primary Key: customer_id\n\tMONGO message type: JSON\n\tMONGO Collection Name: customers\n\tMONGO request timeout in ms: 8277\n\tMONGO retry status code blacklist: 11000\n\tMONGO update only mode: true\n\tMONGO should preserve proto field names: true");

    }
}