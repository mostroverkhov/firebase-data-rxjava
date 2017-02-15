package com.github.mostroverkhov.firebase_rx_data.common;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maksym Ostroverkhov on 15.02.2017.
 */
public class Recorder {
    private final List<Event> events = new ArrayList<>();
    private final List<Event> errors = new ArrayList<>();
    private final List<Event> completes = new ArrayList<>();
    private final List<Event> nexts = new ArrayList<>();

    public Recorder recordNext(Object data) {
        events.add(new Event(Event.Type.NEXT, data));
        nexts.add(new Event(Event.Type.NEXT, data));
        return this;
    }

    public Recorder recordError(Throwable e) {
        events.add(new Event(Event.Type.ERROR, e));
        errors.add(new Event(Event.Type.ERROR, e));
        return this;
    }

    public Recorder recordComplete() {
        events.add(new Event(Event.Type.COMPLETE, null));
        completes.add(new Event(Event.Type.COMPLETE, null));
        return this;
    }

    public Recorder reset() {
        events.clear();
        errors.clear();
        completes.clear();
        nexts.clear();
        return this;
    }

    public List<Event> getEvents() {
        return new ArrayList<>(events);
    }

    public List<Event> getErrors() {
        return new ArrayList<>(errors);
    }

    public List<Event> getCompletes() {
        return new ArrayList<>(completes);
    }

    public List<Event> getNexts() {
        return new ArrayList<>(nexts);
    }

    public static class Event {
        private final Event.Type type;
        private final Object data;

        public Event(Event.Type type, Object data) {
            this.type = type;
            this.data = data;
        }

        public Event.Type getType() {
            return type;
        }

        public <T> T getData() {
            return (T) data;
        }

        public enum Type {
            NEXT, ERROR, COMPLETE
        }
    }
}
