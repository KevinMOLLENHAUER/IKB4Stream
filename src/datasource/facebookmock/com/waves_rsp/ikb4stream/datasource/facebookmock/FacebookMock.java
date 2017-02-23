package com.waves_rsp.ikb4stream.datasource.facebookmock;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.waves_rsp.ikb4stream.core.datasource.model.IDataProducer;
import com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector;
import com.waves_rsp.ikb4stream.core.model.Event;
import com.waves_rsp.ikb4stream.core.model.LatLong;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.TimeZone;

public class FacebookMock implements IProducerConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(FacebookMock.class);
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(FacebookMock.class, "resources/datasource/facebookmock/config.properties");
    private static final String SOURCE = "Facebook";

    public FacebookMock() {
        // Do nothing
    }

    /**
     * Load data registered into a json facebook file and parse them to create event
     *
     * @param dataProducer
     */
    @Override
    public void load(IDataProducer dataProducer) {
        Objects.requireNonNull(dataProducer);
        ObjectMapper mapper = new ObjectMapper();
        JsonParser parser;
        Path path = Paths.get(PROPERTIES_MANAGER.getProperty("facebookmock.path"));

        try (InputStream inputStream = new FileInputStream(path.toString())){
            parser = mapper.getFactory().createParser(inputStream);
            while(!Thread.currentThread().isInterrupted()) {
                try {
                    while(parser.nextToken() == JsonToken.START_OBJECT) {
                        ObjectNode objectNode = mapper.readTree(parser);
                        Event event = getEventFromJson(objectNode);
                        pushIfValidEvent(dataProducer, event);
                    }
                } catch (IOException e) {
                    LOGGER.error("Something went wrong with the facebook post reading");
                    return;
                }finally {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return;
        }
    }

    /**
     * Indicates whether this producer is enabled or not, according to facebookmock.enable
     * @return true is facebookmock.enable is true
     */
    @Override
    public boolean isActive() {
        try {
            return Boolean.valueOf(PROPERTIES_MANAGER.getProperty("facebookmock.enable"));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Parse a json node in order to create a Date object
     *
     * @param jsonNode json to parse to Date
     * @return null if a ParseException has been thrown, else the Date object created
     * @throws ParseException when it cannot parse the json
     */
    static Date getDateFromJson (JsonNode jsonNode) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss-SSS");
        TimeZone tz = TimeZone.getTimeZone("CET");
        String timeString = jsonNode.toString();
        timeString = timeString.substring(1, timeString.length()-4);
        dateFormat.setTimeZone(tz);
        try {
            return dateFormat.parse(timeString);
        } catch (ParseException err) {
            LOGGER.error(err.getMessage());
            return null;
        }
    }

    /**
     * Parse an object node in order to create an Event object
     *
     * @param objectNode object node to convert to Event
     * @return Event converted format
     */
    private static Event getEventFromJson(ObjectNode objectNode) {
        JsonNode startNode = objectNode.findValue("start_time");
        Date startDate = getDateFromJson(startNode);
        JsonNode endNode = objectNode.findValue("end_time");
        Date endDate = getDateFromJson(endNode);
        LatLong latLong = jsonToLatLong(objectNode);
        String description = objectNode.findValue("description").toString();
        return new Event(latLong, startDate, endDate, description, SOURCE);
    }

    /**
     * Push a valid event into the data producer object
     *
     * @param dataProducer
     * @param event
     */
    private static void pushIfValidEvent(IDataProducer dataProducer, Event event) {
        if(event != null) {
            dataProducer.push(event);
            LOGGER.info("Event "+event.toString()+" was correctly pushed");
        }else {
            LOGGER.error("An event was discard (missing field)");
        }
    }

    /**
     * Create LatLong from an ObjectNode object and parse it to get GPS coordinates
     *
     * @param objectNode
     * @return latlong object containing latitude and longitude values
     */
    private static LatLong jsonToLatLong(ObjectNode objectNode) {
        double latitude = objectNode.findValue("latitude").asDouble();
        double longitude = objectNode.findValue("longitude").asDouble();
        return new LatLong(latitude, longitude);
    }
}