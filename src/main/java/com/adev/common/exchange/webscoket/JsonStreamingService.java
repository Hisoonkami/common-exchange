package com.adev.common.exchange.webscoket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;

public abstract class JsonStreamingService extends WebsocketStreamingService<JsonNode>{
    private static final Logger LOG = LoggerFactory.getLogger(JsonStreamingService.class);

    public JsonStreamingService(String apiUrl) {
        super(apiUrl);
    }

    public JsonStreamingService(String apiUrl, int maxFramePayloadLength) {
        super(apiUrl, maxFramePayloadLength);
    }

    public JsonStreamingService(String apiUrl, int maxFramePayloadLength, Duration connectionTimeout,
                                     Duration retryDuration) {
        super(apiUrl, maxFramePayloadLength, connectionTimeout, retryDuration);
    }

    @Override
    public void messageHandler(String message) {

        LOG.debug("Received message: {}", message);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode;

        // Parse incoming message to JSON
        try {

            if(!"pong".equals(message.toLowerCase())){

                jsonNode = objectMapper.readTree(message);
                // In case of array - handle every message separately.
                if (jsonNode.getNodeType().equals(JsonNodeType.ARRAY)) {
                    for (JsonNode node : jsonNode) {

                        handleMessage(node);
                    }
                } else {
                    handleMessage(jsonNode);
                }
            }
        } catch (IOException e) {
            LOG.error("Error parsing incoming message to JSON: {}", message);
            return;
        }

    }
}
