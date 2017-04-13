package com.domhauton.membrane.distributed.block.ledger;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by Dominic Hauton on 02/03/17.
 */
class Evidence {
  private final DateTime start;

  private final List<SaltHashPair> saltHashPairList;

  Evidence(DateTime start, List<SaltHashPair> saltHashPairList) {
    this.start = start;
    this.saltHashPairList = saltHashPairList;
  }

  SaltHashPair getBlockConfirmation(DateTime dateTime) throws NoSuchElementException {
    int hoursFromStart = Hours.hoursBetween(start, dateTime).getHours();
    if (hoursFromStart < 0 || hoursFromStart >= saltHashPairList.size()) {
      throw new NoSuchElementException("There is no block confirmation for time " + dateTime.toString());
    } else {
      return saltHashPairList.get(hoursFromStart);
    }
  }
}
