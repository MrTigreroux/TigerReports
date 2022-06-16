package fr.mrtigreroux.tigerreports.objects.reports;

import java.util.Objects;

/**
 * @author MrTigreroux
 */
public class ReportsPageCharacteristics {

	public final ReportsCharacteristics reportsCharacteristics;
	public final int page;

	public static ReportsPageCharacteristics fromString(String reportsPageCharacteristics) {
		String[] tokens = reportsPageCharacteristics.split(ReportsCharacteristics.REPORTS_CHARACTERISTICS_SEPARATOR);
		return tokens.length >= 4
		        ? new ReportsPageCharacteristics(ReportsCharacteristics.fromStrings(tokens[0], tokens[1], tokens[2]),
		                Integer.valueOf(tokens[3]))
		        : null;
	}

	public ReportsPageCharacteristics(ReportsCharacteristics reportsCharacteristics, int page) {
		this.reportsCharacteristics = Objects.requireNonNull(reportsCharacteristics);
		this.page = page;
	}

	@Override
	public int hashCode() {
		return Objects.hash(page, reportsCharacteristics);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ReportsPageCharacteristics)) {
			return false;
		}
		ReportsPageCharacteristics other = (ReportsPageCharacteristics) obj;
		return page == other.page && Objects.equals(reportsCharacteristics, other.reportsCharacteristics);
	}

	@Override
	public String toString() {
		return String.join(ReportsCharacteristics.REPORTS_CHARACTERISTICS_SEPARATOR, reportsCharacteristics.toString(),
		        Integer.toString(page));
	}

}