package fr.mrtigreroux.tigerreports.objects.reports;

import java.util.Objects;
import java.util.UUID;

/**
 * @author MrTigreroux
 */
public class ReportsCharacteristics {

    public static final ReportsCharacteristics CURRENT_REPORTS = new ReportsCharacteristics(null, null, false);
    public static final ReportsCharacteristics ARCHIVED_REPORTS = new ReportsCharacteristics(null, null, true);
    public static final String REPORTS_CHARACTERISTICS_SEPARATOR = ",";
    private static final String NULL_UUID = "NULL";

    public final UUID reporterUUID;
    public final UUID reportedUUID;
    public final boolean archived;

    public static ReportsCharacteristics fromStrings(String reporterUUID, String reportedUUID, String archived) {
        return new ReportsCharacteristics(!NULL_UUID.equals(reporterUUID) ? UUID.fromString(reporterUUID) : null,
                !NULL_UUID.equals(reportedUUID) ? UUID.fromString(reportedUUID) : null, Boolean.valueOf(archived));
    }

    public ReportsCharacteristics(UUID reporterUUID, UUID reportedUUID, boolean archived) {
        this.reporterUUID = reporterUUID;
        this.reportedUUID = reportedUUID;
        this.archived = archived;
    }

    @Override
    public int hashCode() {
        return Objects.hash(archived, reportedUUID, reporterUUID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReportsCharacteristics)) {
            return false;
        }
        ReportsCharacteristics other = (ReportsCharacteristics) obj;
        return archived == other.archived && Objects.equals(reportedUUID, other.reportedUUID)
                && Objects.equals(reporterUUID, other.reporterUUID);
    }

    @Override
    public String toString() {
        return String.join(REPORTS_CHARACTERISTICS_SEPARATOR,
                reporterUUID != null ? reporterUUID.toString() : NULL_UUID,
                reportedUUID != null ? reportedUUID.toString() : NULL_UUID, Boolean.toString(archived));
    }

}