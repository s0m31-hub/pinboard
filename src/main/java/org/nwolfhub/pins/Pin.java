package org.nwolfhub.pins;

import java.io.Serializable;

public class Pin implements Serializable {

    public Pin(Long from, Long unixWhen, String text) {
        this.from = from;
        this.unixWhen = unixWhen;
        this.text = text;
    }

    public Long from;
    public Long unixWhen;
    public String text;

    public Long getFrom() {
        return from;
    }

    public Pin setFrom(Long from) {
        this.from = from;
        return this;
    }

    public Long getUnixWhen() {
        return unixWhen;
    }

    public Pin setUnixWhen(Long unixWhen) {
        this.unixWhen = unixWhen;
        return this;
    }

    public String getText() {
        return text;
    }

    public Pin setText(String text) {
        this.text = text;
        return this;
    }

    public boolean checkMe() {
        return System.currentTimeMillis()/1000L>this.unixWhen;
    }
}
