package org.entcore.broker.api.dto.audience;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class represents a response indicating whether a user has access to specific resources.
 * It contains information about the success of the check, access status, and potential error messages.
 */
public class CheckResourceAccessResponseDTO {
    /**
     * Indicates if the check operation was successful.
     */
    private final boolean success;
    
    /**
     * Contains an error message if the check operation failed.
     */
    private final String errorMsg;
    
    /**
     * Indicates if the user has access to the requested resources.
     * Only valid if success is true.
     */
    private final boolean access;

    /**
     * Creates a new instance of CheckResourceAccessResponseDTO for a successful operation.
     *
     * @param access Indicates if the user has access to the requested resources.
     */
    public CheckResourceAccessResponseDTO(boolean access) {
        this.success = true;
        this.errorMsg = null;
        this.access = access;
    }

    /**
     * Creates a new instance of CheckResourceAccessResponseDTO for a failed operation.
     *
     * @param errorMsg The error message explaining why the operation failed.
     */
    public CheckResourceAccessResponseDTO(String errorMsg) {
        this.success = false;
        this.errorMsg = errorMsg;
        this.access = false;
    }

    /**
     * Creates a new instance of CheckResourceAccessResponseDTO.
     *
     * @param success Indicates if the check operation was successful.
     * @param errorMsg The error message if the operation failed.
     * @param access Indicates if the user has access to the requested resources.
     */
    @JsonCreator
    public CheckResourceAccessResponseDTO(
            @JsonProperty("success") boolean success,
            @JsonProperty("errorMsg") String errorMsg,
            @JsonProperty("access") boolean access) {
        this.success = success;
        this.errorMsg = errorMsg;
        this.access = access;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public boolean isAccess() {
        return access;
    }

    @Override
    public String toString() {
        return "CheckResourceAccessResponseDTO{" +
                "success=" + success +
                ", errorMsg='" + (errorMsg != null ? errorMsg : "none") + '\'' +
                ", access=" + access +
                '}';
    }
}