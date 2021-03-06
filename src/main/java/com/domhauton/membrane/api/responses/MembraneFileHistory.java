package com.domhauton.membrane.api.responses;

import java.util.List;

/**
 * Created by dominic on 05/02/17.
 */
public class MembraneFileHistory implements MembraneResponse {
  private final String filePath;
  private final List<FileHistoryEntry> fileHistoryEntryList;

  public MembraneFileHistory(List<FileHistoryEntry> fileHistoryEntryList, String filePath) {
    this.fileHistoryEntryList = fileHistoryEntryList;
    this.filePath = filePath;
  }

  public String getFilePath() {
    return filePath;
  }

  public List<FileHistoryEntry> getFileHistory() {
    return fileHistoryEntryList;
  }
}
