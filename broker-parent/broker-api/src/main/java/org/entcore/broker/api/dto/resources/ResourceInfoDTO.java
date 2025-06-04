package org.entcore.broker.api.dto.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;

/**
 * DTO containing detailed information about a resource.
 * Provides metadata about a resource such as title, author, and timestamps.
 */
public class ResourceInfoDTO {
  /**
   * The unique identifier of the resource.
   */
  private final String id;
  
  /**
   * The title of the resource.
   */
  private final String title;
  
  /**
   * The description of the resource.
   */
  private final String description;
  
  /**
   * The URL of the thumbnail image for the resource.
   */
  private final String thumbnail;
  
  /**
   * The name of the author of the resource.
   */
  private final String authorName;
  
  /**
   * The unique identifier of the author of the resource.
   */
  private final String authorId;
  
  /**
   * The date when the resource was created.
   */
  private final Date creationDate;
  
  /**
   * The date when the resource was last modified.
   */
  private final Date modificationDate;
  
  /**
   * Creates a new ResourceInfoDTO with the given information.
   * 
   * @param id The unique identifier of the resource.
   * @param title The title of the resource.
   * @param description The description of the resource.
   * @param thumbnail The URL of the thumbnail image for the resource.
   * @param authorName The name of the author of the resource.
   * @param authorId The unique identifier of the author of the resource.
   * @param creationDate The date when the resource was created.
   * @param modificationDate The date when the resource was last modified.
   */
  @JsonCreator
  public ResourceInfoDTO(
      @JsonProperty("id") String id,
      @JsonProperty("title") String title,
      @JsonProperty("description") String description,
      @JsonProperty("thumbnail") String thumbnail,
      @JsonProperty("authorName") String authorName,
      @JsonProperty("authorId") String authorId,
      @JsonProperty("creationDate") Date creationDate,
      @JsonProperty("modificationDate") Date modificationDate) {
    this.id = id;
    this.title = title;
    this.description = description;
    this.thumbnail = thumbnail;
    this.authorName = authorName;
    this.authorId = authorId;
    this.creationDate = creationDate;
    this.modificationDate = modificationDate;
  }
  
  /**
   * Gets the unique identifier of the resource.
   * @return The resource ID.
   */
  public String getId() {
    return id;
  }
  
  /**
   * Gets the title of the resource.
   * @return The resource title.
   */
  public String getTitle() {
    return title;
  }
  
  /**
   * Gets the description of the resource.
   * @return The resource description.
   */
  public String getDescription() {
    return description;
  }
  
  /**
   * Gets the URL of the thumbnail image for the resource.
   * @return The thumbnail URL.
   */
  public String getThumbnail() {
    return thumbnail;
  }
  
  /**
   * Gets the name of the author of the resource.
   * @return The author name.
   */
  public String getAuthorName() {
    return authorName;
  }
  
  /**
   * Gets the unique identifier of the author of the resource.
   * @return The author ID.
   */
  public String getAuthorId() {
    return authorId;
  }
  
  /**
   * Gets the date when the resource was created.
   * @return The creation date.
   */
  public Date getCreationDate() {
    return creationDate;
  }
  
  /**
   * Gets the date when the resource was last modified.
   * @return The modification date.
   */
  public Date getModificationDate() {
    return modificationDate;
  }
  
  @Override
  public String toString() {
    return "ResourceInfoDTO{" +
            "id='" + id + '\'' +
            ", title='" + title + '\'' +
            ", authorName='" + authorName + '\'' +
            ", authorId='" + authorId + '\'' +
            ", creationDate=" + creationDate +
            ", modificationDate=" + modificationDate +
            '}';
  }
}