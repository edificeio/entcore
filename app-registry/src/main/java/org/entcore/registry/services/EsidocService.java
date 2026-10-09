package org.entcore.registry.services;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

import java.util.List;

public interface EsidocService {

    /**
     * Books currently borrowed by a borrower in the libraries of his structures, the most urgent first.
     * A structure where e-sidoc does not know the borrower adds no loan.
     *
     * @param borrowerId id of the borrower in e-sidoc: the external id of the user, exported to BCDI as IDENTITE_ENT_M
     * @param uais       UAI of the structures of the borrower
     * @return the loans {@code {id, title, dueDate, overdue}}, failed only when e-sidoc failed for every structure
     */
    Future<JsonArray> getLoans(String borrowerId, List<String> uais);

    /**
     * Number of books currently borrowed by each child, without any detail on the books.
     *
     * @param childrenIds ids of the children
     * @return {@code {id, firstName, loansCount}} for each child attached to a structure
     */
    Future<JsonArray> getChildrenLoansCount(List<String> childrenIds);
}
