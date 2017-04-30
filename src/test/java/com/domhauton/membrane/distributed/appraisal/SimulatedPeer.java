package com.domhauton.membrane.distributed.appraisal;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;

import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Created by dominic on 30/04/17.
 */
class SimulatedPeer {

  private final String name;
  private final int weekdayStart;
  private final int weekdayEnd;
  private final int weekendStart;
  private final int weekendEnd;
  private final LinkedList<Double> ratings;

  SimulatedPeer(String name, int weekdayStart, int weekdayEnd, int weekendStart, int weekendEnd) {
    this.name = name;
    this.weekdayStart = weekdayStart;
    this.weekdayEnd = weekdayEnd;
    this.weekendStart = weekendStart;
    this.weekendEnd = weekendEnd;
    ratings = new LinkedList<>();
  }

  boolean isOnline(DateTime dateTime) {
    int hourOfWeek = hourOfWeek(dateTime);
    int dayOfWeek = hourOfWeek / DateTimeConstants.HOURS_PER_DAY;
    int hourOfDay = hourOfWeek % DateTimeConstants.HOURS_PER_DAY;
    if (dayOfWeek < 2) { // Weekend
      return (hourOfDay >= weekendStart) && (hourOfDay <= weekendEnd);
    } else { // Weekday
      return (hourOfDay >= weekdayStart) && (hourOfDay <= weekdayEnd);
    }
  }

  void addAppraisal(Double appraisal) {
    ratings.add(appraisal);
  }

  static int hourOfWeek(DateTime dateTime) {
    return dateTime.getHourOfDay() + (dateTime.getDayOfWeek() - 1) * DateTimeConstants.HOURS_PER_DAY;
  }

  void printAppraisalCSV() {
    System.out.println(name + "," + ratings.stream().map(Object::toString).collect(Collectors.joining(",")));
  }

  String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SimulatedPeer that = (SimulatedPeer) o;

    if (weekdayStart != that.weekdayStart) return false;
    if (weekdayEnd != that.weekdayEnd) return false;
    if (weekendStart != that.weekendStart) return false;
    if (weekendEnd != that.weekendEnd) return false;
    if (getName() != null ? !getName().equals(that.getName()) : that.getName() != null) return false;
    return ratings != null ? ratings.equals(that.ratings) : that.ratings == null;
  }

  @Override
  public int hashCode() {
    int result = getName() != null ? getName().hashCode() : 0;
    result = 31 * result + weekdayStart;
    result = 31 * result + weekdayEnd;
    result = 31 * result + weekendStart;
    result = 31 * result + weekendEnd;
    result = 31 * result + (ratings != null ? ratings.hashCode() : 0);
    return result;
  }
}
