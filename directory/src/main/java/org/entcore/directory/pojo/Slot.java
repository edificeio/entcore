package org.entcore.directory.pojo;

import fr.wseduc.webutils.http.Renders;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.logging.impl.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Slot {
    protected static final Logger log = LoggerFactory.getLogger(Renders.class);

    public static final String NAME = "name";
    public static final String START_HOUR = "startHour";
    public static final String END_HOUR = "endHour";
    public static final String ID = "id";

    private SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm");

    JsonObject slotAsJson;

    public Slot(JsonObject slot) {
        this.slotAsJson = slot;
    }

    public String getName() {
        if (slotAsJson == null) {
            return "";
        }
        return slotAsJson.getString(NAME, "");
    }

    public Long getStart() {
        if (slotAsJson == null) {
            return null;
        }
        return parse(slotAsJson.getString(START_HOUR));
    }

    public Long getEnd() {
        if (slotAsJson == null) {
            return null;
        }
        return parse(slotAsJson.getString(END_HOUR));
    }

    private Long parse(String hourfromJson) {
        try {
            return dateFormatter.parse(hourfromJson).getTime();
        } catch (ParseException e) {
            log.warn("Slot Date could not be parsed", e);
        }
        return null;
    }


    public boolean overlap(Slot givenSlot) {
        Long start = this.getStart();
        Long end = this.getEnd();
        Long givenSlotStart = givenSlot.getStart();
        Long givenSlotEnd = givenSlot.getEnd();
        return (start != null)
                && (end != null)
                && (givenSlotStart != null)
                && (givenSlotEnd != null)
                && (start < givenSlotEnd)
                && (givenSlotStart < end);
    }

    public String getId() {
        return slotAsJson.getString(ID);
    }
}
