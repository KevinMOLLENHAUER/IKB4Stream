package com.waves_rsp.ikb4stream.core.communication.model;

import org.junit.Test;

public class RequestTest {

    @Test(expected = NullPointerException.class)
    public void nullRequest() {
        new Request(null, null, null, null);
    }
}