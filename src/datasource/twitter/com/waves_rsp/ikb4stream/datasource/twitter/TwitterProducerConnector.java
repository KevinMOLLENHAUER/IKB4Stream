/*
 * Copyright (C) 2017 ikb4stream team
 * ikb4stream is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * ikb4stream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 *
 */

package com.waves_rsp.ikb4stream.datasource.twitter;

import com.waves_rsp.ikb4stream.core.datasource.model.IDataProducer;
import com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector;
import com.waves_rsp.ikb4stream.core.model.Event;
import com.waves_rsp.ikb4stream.core.model.LatLong;
import com.waves_rsp.ikb4stream.core.model.PropertiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;

import java.util.*;

/**
 * Listen any events provided by the twitter api and load them into a IDataProducer object.
 *
 * @author ikb4stream
 * @version 1.0
 * @see com.waves_rsp.ikb4stream.core.datasource.model.IProducerConnector
 */
public class TwitterProducerConnector implements IProducerConnector {
    /**
     * Properties of this module
     *
     * @see PropertiesManager
     * @see PropertiesManager#getProperty(String)
     * @see PropertiesManager#getInstance(Class, String)
     */
    private static final PropertiesManager PROPERTIES_MANAGER = PropertiesManager.getInstance(TwitterProducerConnector.class, "resources/datasource/twitter/config.properties");
    /**
     * Logger used to log all information in this module
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TwitterProducerConnector.class);
    /**
     * Configuration to use Twitter Stream API
     *
     * @see TwitterProducerConnector#loadTwitterProperties()
     */
    private final ConfigurationBuilder confBuilder = new ConfigurationBuilder();
    /**
     * Source name of corresponding {@link Event}
     *
     * @see TwitterStreamListener#onStatus(Status)
     */
    private final String source = PROPERTIES_MANAGER.getProperty("twitter.source");
    /**
     * Latitude max of {@link com.waves_rsp.ikb4stream.core.communication.model.BoundingBox BoundingBox}
     *
     * @see TwitterStreamListener#getLatLong(Status)
     */
    private final double latitudeMax;
    /**
     * Latitude min of {@link com.waves_rsp.ikb4stream.core.communication.model.BoundingBox BoundingBox}
     *
     * @see TwitterStreamListener#getLatLong(Status)
     */
    private final double latitudeMin;
    /**
     * Longitude max of {@link com.waves_rsp.ikb4stream.core.communication.model.BoundingBox BoundingBox}
     *
     * @see TwitterStreamListener#getLatLong(Status)
     */
    private final double longitudeMax;
    /**
     * Longitude min of {@link com.waves_rsp.ikb4stream.core.communication.model.BoundingBox BoundingBox}
     *
     * @see TwitterStreamListener#getLatLong(Status)
     */
    private final double longitudeMin;
    /**
     * Represent bounding to analyse
     *
     * @see TwitterProducerConnector#load(IDataProducer)
     */
    private final double[][] boundingBox;

    /**
     * Instantiate the {@link TwitterProducerConnector} object with load properties
     *
     * @see TwitterProducerConnector#latitudeMax
     * @see TwitterProducerConnector#latitudeMin
     * @see TwitterProducerConnector#longitudeMax
     * @see TwitterProducerConnector#longitudeMin
     * @see TwitterProducerConnector#boundingBox
     */
    public TwitterProducerConnector() {
        loadTwitterProperties();
        try {
            latitudeMax = Double.valueOf(PROPERTIES_MANAGER.getProperty("twitter.latitude.maximum"));
            latitudeMin = Double.valueOf(PROPERTIES_MANAGER.getProperty("twitter.latitude.minimum"));
            longitudeMax = Double.valueOf(PROPERTIES_MANAGER.getProperty("twitter.longitude.maximum"));
            longitudeMin = Double.valueOf(PROPERTIES_MANAGER.getProperty("twitter.longitude.minimum"));
            boundingBox = new double[][]{
                    {longitudeMin, latitudeMin},
                    {longitudeMax, latitudeMax}
            };
        } catch (IllegalArgumentException e) {
            LOGGER.error("Invalid properties {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    /**
     * Listen tweets from twitter with a bounding box and load them with the data producer object
     *
     * @param dataProducer {@link IDataProducer} contains the data queue
     * @see TwitterProducerConnector#confBuilder
     * @see TwitterProducerConnector#boundingBox
     */
    @Override
    public void load(IDataProducer dataProducer) {
        Objects.requireNonNull(dataProducer);
        TwitterStream twitterStream = null;
        try {
            TwitterStreamListener streamListener = new TwitterStreamListener(dataProducer);
            twitterStream = new TwitterStreamFactory(confBuilder.build()).getInstance();
            FilterQuery filterQuery = new FilterQuery();
            filterQuery.locations(boundingBox);
            twitterStream.addListener(streamListener);
            twitterStream.filter(filterQuery);
            Thread.currentThread().join();
        } catch (IllegalArgumentException | IllegalStateException err) {
            LOGGER.error("Error loading : " + err.getMessage());
            throw new IllegalStateException(err.getMessage());
        } catch (InterruptedException e) {
            LOGGER.info("Close twitter");
            Thread.currentThread().interrupt();
        } finally {
            if (twitterStream != null) {
                twitterStream.cleanUp();
                twitterStream.shutdown();
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if this jar is active
     *
     * @return true if it should be started
     * @see TwitterProducerConnector#PROPERTIES_MANAGER
     */
    @Override
    public boolean isActive() {
        try {
            return Boolean.valueOf(PROPERTIES_MANAGER.getProperty("twitter.enable"));
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * Load properties for twitter connector
     *
     * @see TwitterProducerConnector#confBuilder
     * @see TwitterProducerConnector#PROPERTIES_MANAGER
     */
    private void loadTwitterProperties() {
        try {
            confBuilder.setOAuthAccessToken(PROPERTIES_MANAGER.getProperty("twitter.key.auth.accesstoken"));
            confBuilder.setOAuthAccessTokenSecret(PROPERTIES_MANAGER.getProperty("twitter.secret.auth.accesstoken"));
            confBuilder.setOAuthConsumerKey(PROPERTIES_MANAGER.getProperty("twitter.key.consumer.accesstoken"));
            confBuilder.setOAuthConsumerSecret(PROPERTIES_MANAGER.getProperty("twitter.secret.consumer.accesstoken"));
            confBuilder.setJSONStoreEnabled(true);
        } catch (IllegalArgumentException err) {
            LOGGER.error("Load Twitter Properties {} ", err.getMessage());
            throw new IllegalStateException(err.getMessage());
        }
    }

    /**
     * A status listener in order to get tweets with the method onStatus
     *
     * @author ikb4stream
     * @version 1.0
     */
    private class TwitterStreamListener implements StatusListener {
        /**
         * Copy of {@link IDataProducer} from {@link TwitterProducerConnector#load(IDataProducer)}
         */
        private final IDataProducer dataProducer;

        /**
         * Instantiate a Twitter listener with a copy of {@link IDataProducer}
         *
         * @param dataProducer {@link IDataProducer} from {@link TwitterProducerConnector}
         */
        private TwitterStreamListener(IDataProducer dataProducer) {
            this.dataProducer = dataProducer;
        }

        /**
         * Method called when a new Tweet is pushed in Twitter Stream API
         *
         * @param status Represent a Tweet
         * @see TwitterProducerConnector#source
         * @see TwitterStreamListener#dataProducer
         */
        @Override
        public void onStatus(Status status) {
            String description = status.getText();
            Date start = status.getCreatedAt();
            Date end = status.getCreatedAt();
            User user = status.getUser();
            LatLong[] latLong = getLatLong(status);
            if (latLong.length > 0) {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.append("description", description);
                    jsonObject.append("user_certified", user.isVerified());
                    Event event;
                    if (latLong.length == 1) {
                        event = new Event(latLong[0], start, end, jsonObject.toString(), source);
                    } else {
                        event = new Event(latLong, start, end, jsonObject.toString(), source);
                    }
                    this.dataProducer.push(event);
                } catch (JSONException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        /**
         * Get position of Tweet
         *
         * @param status Tweet to analyse
         * @return {@link LatLong} to represent position of Tweet
         * @see TwitterProducerConnector#longitudeMin
         * @see TwitterProducerConnector#longitudeMax
         * @see TwitterProducerConnector#latitudeMax
         * @see TwitterProducerConnector#latitudeMin
         */
        private LatLong[] getLatLong(Status status) {
            if (status.getGeoLocation() != null) {
                LOGGER.info("Status geolocation found !");
                return new LatLong[]{new LatLong(status.getGeoLocation().getLatitude(), status.getGeoLocation().getLongitude())};
            } else if (status.getPlace() != null && status.getPlace().getBoundingBoxCoordinates() != null) {
                return getLatLongFromBoudingBox(status.getPlace().getBoundingBoxCoordinates());
            } else {
                LOGGER.info("Status geolocation cannot be found !");
                return new LatLong[]{
                        new LatLong(latitudeMin, longitudeMin),
                        new LatLong(latitudeMax, longitudeMin),
                        new LatLong(latitudeMax, longitudeMax),
                        new LatLong(latitudeMin, longitudeMax),
                        new LatLong(latitudeMin, longitudeMin)
                };
            }
        }

        /**
         * Get an array of {@link LatLong} from GeoLocation object of Status
         *
         * @param geoLocations Array of GeoLocation
         * @return Array of {@link LatLong}
         */
        private LatLong[] getLatLongFromBoudingBox(GeoLocation[][] geoLocations) {
            List<LatLong> latLongList = new ArrayList<>();
            Arrays.stream(geoLocations)
                    .forEach(arrayGeo -> Arrays.stream(arrayGeo)
                            .forEach(geo -> latLongList.add(new LatLong(geo.getLatitude(), geo.getLongitude()))));
            LatLong[] latLong = new LatLong[latLongList.size() + 1];
            for (int i = 0; i < latLongList.size(); i++) {
                latLong[i] = latLongList.get(i);
            }
            latLong[latLong.length - 1] = latLong[0];
            return latLong;
        }

        /**
         * Called upon deletionNotice notices. Clients are urged to honor deletionNotice requests and discard deleted statuses immediately. At times, status deletionNotice messages may arrive before the status. Even in this case, the late arriving status should be deleted from your backing store.
         *
         * @param statusDeletionNotice the deletionNotice notice
         */
        @Override
        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            LOGGER.info("Deletion {}", statusDeletionNotice.getStatusId());
        }

        /**
         * This notice will be sent each time a limited stream becomes unlimited.<br>
         * If this number is high and or rapidly increasing, it is an indication that your predicate is too broad, and you should consider a predicate with higher selectivity.
         *
         * @param numberOfLimitedStatuses an enumeration of statuses that matched the track predicate but were administratively limited.
         */
        @Override
        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            LOGGER.info("number of limited status : {}", numberOfLimitedStatuses);

        }

        /**
         * Called upon location deletion messages. Clients are urged to honor deletion requests and remove appropriate geolocation information from both the display and your backing store immediately. Note that in some cases the location deletion message may arrive before a tweet that lies within the deletion range arrives. You should still strip the location data.
         *
         * @param userId       user id
         * @param upToStatusId up to status id
         */
        @Override
        public void onScrubGeo(long userId, long upToStatusId) {
            LOGGER.info("user id : " + userId + ", " + upToStatusId);
        }

        /**
         * Called when receiving stall warnings.
         *
         * @param warning StallWaning
         */
        @Override
        public void onStallWarning(StallWarning warning) {
            LOGGER.warn(warning.getMessage());
        }

        /**
         * When exception append
         *
         * @param ex Exception to catch
         */
        @Override
        public void onException(Exception ex) {
            LOGGER.error(ex.getMessage());
        }
    }
}
