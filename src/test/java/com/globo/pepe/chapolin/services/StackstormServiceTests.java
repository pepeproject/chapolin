package com.globo.pepe.chapolin.services;


import static com.globo.pepe.chapolin.services.RequestService.X_PEPE_TRIGGER_HEADER;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServer;
import static com.globo.pepe.chapolin.suites.PepeSuiteTests.mockApiServerApiKeyCreated;
import static com.globo.pepe.common.util.Constants.PACK_NAME;
import static com.globo.pepe.common.util.Constants.TRIGGER_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.globo.pepe.chapolin.configuration.HttpClientConfiguration;
import com.globo.pepe.common.model.Event;
import com.globo.pepe.common.model.Metadata;
import com.globo.pepe.common.services.JsonLoggerService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
        "pepe.logging.tags=default",
        "pepe.chapolin.stackstorm.api=http://127.0.0.1:9101/api/v1",
        "pepe.chapolin.stackstorm.auth=http://127.0.0.1:9100/auth/v1",
        "pepe.chapolin.stackstorm.login=u_pepe",
        "pepe.chapolin.stackstorm.password=u_pepe",
        "pepe.chapolin.sleep_interval_on_fail=1"
})
@ContextConfiguration(classes = {
    StackstormService.class,
    RequestService.class,
    JsonLoggerService.class,
    ObjectMapper.class,
    JsonSchemaGeneratorService.class,
    HttpClientConfiguration.class,
    StackstormAuthService.class
}, loader = AnnotationConfigContextLoader.class)
public class StackstormServiceTests {

    private static final String TRIGGER_FULL_PREFIX = PACK_NAME + "." + TRIGGER_PREFIX;

    private static final String PROJECT = "pepe";

    private static final String TRIGGER_NAME_CREATED = "triggerNameOK";
    private static final String TRIGGER_FULL_NAME_CREATED = TRIGGER_FULL_PREFIX + "." + PROJECT + "." + TRIGGER_NAME_CREATED;

    private static final String TRIGGER_NAME_NOT_CREATED = "triggerNameNotCreated";
    private static final String TRIGGER_FULL_NAME_NOT_CREATED = TRIGGER_FULL_PREFIX + "." + PROJECT + "." + TRIGGER_NAME_NOT_CREATED;

    private static final String TRIGGER_FULL_NAME_EMPTY = TRIGGER_FULL_PREFIX + ".";

    @Autowired
    public StackstormService stackstormService;

    @Autowired
    private ObjectMapper mapper;

    public void mockSendTriggerCreated() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExists = StackstormServiceTests.class.getResourceAsStream("/trigger-exists.json");
        String bodyTriggerExists = IOUtils.toString(triggerExists, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ TRIGGER_FULL_NAME_CREATED))
                .respond(response().withBody(bodyTriggerExists).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(200));

        InputStream eventSent = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSent = IOUtils.toString(eventSent, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_CREATED))
                .respond(response().withBody(bodyEventSent).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));

    }

    public void mockSendTriggerNotCreated() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/" + TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-created.json");
        String bodyTriggerCreated = IOUtils.toString(triggerCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyTriggerCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(201));

        InputStream eventSentWithNewTrigger = StackstormServiceTests.class.getResourceAsStream("/event-sent.json");
        String bodyEventSentWithNewTrigger = IOUtils.toString(eventSentWithNewTrigger, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyEventSentWithNewTrigger).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(202));

    }

    public void mockCreateTriggerWithoutName() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerNotFoundWithoutName = StackstormServiceTests.class.getResourceAsStream("/trigger-without-name.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerNotFoundWithoutName, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ TRIGGER_FULL_NAME_EMPTY))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerNotCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-creation-failed-without-name.json");
        String bodyTriggerNotCreated = IOUtils.toString(triggerNotCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_EMPTY))
                .respond(response().withBody(bodyTriggerNotCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(400));

    }

    public void mockTriggerExist500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ TRIGGER_FULL_NAME_CREATED))
            .respond(response().withStatusCode(500));
    }

    public void mockCreateTrigger500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/"+ TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes"))
                .respond(response().withStatusCode(500));
    }

    public void mockSendTrigger500() throws IOException {
        mockApiServer.reset();

        mockApiServerApiKeyCreated();

        InputStream triggerExistsFail = StackstormServiceTests.class.getResourceAsStream("/trigger-exists-fail.json");
        String bodyTriggerExistsFail = IOUtils.toString(triggerExistsFail, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("GET").withPath("/api/v1/triggertypes/" + TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyTriggerExistsFail).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(404));

        InputStream triggerCreated = StackstormServiceTests.class.getResourceAsStream("/trigger-created.json");
        String bodyTriggerCreated = IOUtils.toString(triggerCreated, Charset.defaultCharset());
        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/triggertypes").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withBody(bodyTriggerCreated).withHeader("Content-Type", APPLICATION_JSON_VALUE).withStatusCode(201));

        mockApiServer.when(request().withMethod("POST").withPath("/api/v1/webhooks/st2").withHeader(X_PEPE_TRIGGER_HEADER,
            TRIGGER_FULL_NAME_NOT_CREATED))
                .respond(response().withStatusCode(500));
    }

    @Test
    public void sendTriggerCreatedTest() throws IOException {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_CREATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerCreated();

        assertTrue(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test
    public void sendTriggerNotCreatedTest() throws IOException {
        Event event = new Event();
        event.setId("2");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_NOT_CREATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTriggerNotCreated();

        assertTrue(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test
    public void createTriggerWithoutNameTest() throws IOException {
        Event event = new Event();
        event.setId("3");

        Metadata metadata = new Metadata();
        metadata.setTriggerName("");
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockCreateTriggerWithoutName();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));
    }

    @Test(expected = RuntimeException.class)
    public void sendToTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_CREATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockTriggerExist500();

        stackstormService.sender(mapper.valueToTree(event)).createTriggerIfNecessary();
    }

    @Test
    public void createTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_NOT_CREATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockCreateTrigger500();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));

    }

    @Test
    public void sendTrigger500Test() throws Exception {
        Event event = new Event();
        event.setId("1");

        Metadata metadata = new Metadata();
        metadata.setTriggerName(TRIGGER_NAME_NOT_CREATED);
        metadata.setProject(PROJECT);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("attribute1", "value1");

        event.setMetadata(metadata);
        event.setPayload(payload);

        mockSendTrigger500();

        assertFalse(stackstormService.send(mapper.valueToTree(event)));
    }

}
