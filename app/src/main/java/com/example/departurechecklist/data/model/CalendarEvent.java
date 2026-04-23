package com.example.departurechecklist.data.model;

/**
 * Plain Java model representing a calendar event loaded from CalendarContract.
 */
public class CalendarEvent {
    public static final String TAG = "CalendarEvent";

    private final long id;
    private final String title;
    private final long startTimeMillis;
    private final long endTimeMillis;
    private final String location;
    private final boolean allDay;

    /**
     * Creates a calendar event model.
     *
     * @param id event identifier
     * @param title event title
     * @param startTimeMillis event start time in milliseconds
     * @param endTimeMillis event end time in milliseconds
     * @param location event location
     * @param allDay true when the event is all day
     */
    public CalendarEvent(long id, String title, long startTimeMillis, long endTimeMillis,
            String location, boolean allDay) {
        this.id = id;
        this.title = title;
        this.startTimeMillis = startTimeMillis;
        this.endTimeMillis = endTimeMillis;
        this.location = location;
        this.allDay = allDay;
    }

    /** @return event id */
    public long getId() { return id; }

    /** @return event title */
    public String getTitle() { return title; }

    /** @return start time in milliseconds */
    public long getStartTimeMillis() { return startTimeMillis; }

    /** @return end time in milliseconds */
    public long getEndTimeMillis() { return endTimeMillis; }

    /** @return nullable location */
    public String getLocation() { return location; }

    /** @return true when the event is all day */
    public boolean isAllDay() { return allDay; }
}
