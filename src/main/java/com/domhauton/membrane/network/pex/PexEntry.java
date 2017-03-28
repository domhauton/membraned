package com.domhauton.membrane.network.pex;

import org.joda.time.DateTime;

/**
 * Created by dominic on 28/03/17.
 */
public class PexEntry {
  private final static String SEP = ",";

  private final String address;
  private final int port;
  private final boolean publicEntry;
  private final DateTime lastUpdateDateTime;

  public PexEntry(String address, int port, boolean publicEntry, DateTime lastUpdateDateTime) {
    this.address = address;
    this.port = port;
    this.publicEntry = publicEntry;
    this.lastUpdateDateTime = lastUpdateDateTime;
  }

  public String getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }

  public boolean isPublicEntry() {
    return publicEntry;
  }

  public DateTime getLastUpdateDateTime() {
    return lastUpdateDateTime;
  }

  String serialize() {
    return address + SEP +
        port + SEP +
        (publicEntry ? "PUB" : "PRIV") + SEP +
        lastUpdateDateTime.getMillis();
  }

  static PexEntry deserialize(String entry) throws PexException {
    String[] splitEntry = entry.split(SEP);
    if (splitEntry.length == 4) {
      try {
        // IP Address format unchecked. Assume user does not try to sabotage application...

        String ip = splitEntry[0];
        boolean isPublic = splitEntry[2].equalsIgnoreCase("PUB");
        int port = Integer.parseInt(splitEntry[1]);
        long millis = Long.parseLong(splitEntry[3]);
        DateTime dateTime = new DateTime(millis);

        if (port < 1 || port > 65535) {
          throw new PexException("Invalid port given");
        }

        return new PexEntry(ip, port, isPublic, dateTime);
      } catch (NumberFormatException e) {
        throw new PexException("Could not parse numbers of PEX Entry. " + entry, e);
      } catch (Exception e) {
        throw new PexException("Unexpected Exception while parsing pex entry. " + entry, e);
      }
    } else {
      throw new PexException("Could not deserialize. Required form: 'IP,PORT,PUB/PRIV,millis'" + entry);
    }
  }
}
