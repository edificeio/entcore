

package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
/**
 * This class represents a response to a request to upsert shares for a group.
 * It contains a list of shares for the resource.
 */
public class UpsertGroupSharesResponseDTO {
  /**
   * A list of shares for the resource.
   * Each share is represented by a SharesResponseDTO object.
   */
  private final List<SharesResponseDTO> shares;

  @JsonCreator
  public UpsertGroupSharesResponseDTO(@JsonProperty("shares") List<SharesResponseDTO> shares) {
    this.shares = shares;
  }

  /**
   * Gets the list of shares for the resource.
   * @return A list of SharesResponseDTO objects representing the shares.
   */
  public List<SharesResponseDTO> getShares() {
    return shares;
  }
}
