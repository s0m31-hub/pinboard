package org.nwolfhub.pins;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class PinboardUser implements Serializable {
    public Long id;

    public List<Pin> getPins() {
        return pins;
    }

    public List<Pin> pins;

    public String getState() {
        return state;
    }

    public PinboardUser setState(String state) {
        this.state = state;
        return this;
    }

    public String state;

    public PinboardUser(Long id) {
        this.id = id;
        this.pins = new ArrayList<>();
        this.state = "none";
    }

    public void removePin(Pin p) {
        pins.remove(p);
    }

    public Long getId() {
        return id;
    }

    public PinboardUser setId(Long id) {
        this.id = id;
        return this;
    }

    public List<Pin> getReadyPins() {
        List<Pin> readyPins = new ArrayList<>();
        for(Pin pin:pins) {
            if(pin.checkMe()) {
                readyPins.add(pin);
            }
        }
        return readyPins;
    }

    public void removeExpiredPins() {
        pins.removeIf(Pin::checkMe);
    }

    public Pin getPin(Integer id) {
        return pins.get(id);
    }

    public void addPin(Pin pin) {
        pins.add(pin);
    }

}
