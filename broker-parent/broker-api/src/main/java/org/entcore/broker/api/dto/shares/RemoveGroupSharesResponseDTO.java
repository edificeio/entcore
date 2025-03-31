

package org.entcore.broker.api.dto.shares;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This class represents a response to a request to remove shares from a group.
 * It contains a list of shares that still exist after the removal operation.
 */
public class RemoveGroupSharesResponseDTO {
  /**
   * A list of shares that still exist after the removal operation.
   * Each share is represented by a SharesResponseDTO object.
   */
  private final List<SharesResponseDTO> shares;

  @JsonCreator
  public RemoveGroupSharesResponseDTO(@JsonProperty("shares") List<SharesResponseDTO> shares) {
    this.shares = shares;
  }

  /**
   * Gets the list of shares that still exist after the removal operation.
   * @return A list of SharesResponseDTO objects representing the shares.
   */
  public List<SharesResponseDTO> getShares() {
    return shares;
  }
}
