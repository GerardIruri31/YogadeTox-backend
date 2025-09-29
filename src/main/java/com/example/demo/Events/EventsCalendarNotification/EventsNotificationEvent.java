package com.example.demo.Events.EventsCalendarNotification;

import com.example.demo.client.domain.Client;
import com.example.demo.reunion_temp.domain.Reunion;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
    public class EventsNotificationEvent extends ApplicationEvent {
    private final Reunion reunion;
    private final Client client;

    public EventsNotificationEvent(Object source, Reunion reunion, Client client) {
        super(source);
        this.reunion = reunion;
        this.client = client;
    }
}