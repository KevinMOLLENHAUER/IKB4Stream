package com.waves_rsp.ikb4stream.consumer.model;

import com.waves_rsp.ikb4stream.core.communication.model.Response;
import org.junit.Test;

public class ResponseTest {
    @Test(expected = NullPointerException.class)
    public void nullResponse() {
        new Response(null, null);
    }
}
