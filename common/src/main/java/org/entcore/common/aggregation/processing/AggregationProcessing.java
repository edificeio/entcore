/*
 * Copyright © "Open Digital Education", 2016
 *
 * This program is published by "Open Digital Education".
 * You must indicate the name of the software and the company in any production /contribution
 * using the software and indicate on the home page of the software industry in question,
 * "powered by Open Digital Education" with a reference to the website: https://opendigitaleducation.com/.
 *
 * This program is free software, licensed under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, version 3 of the License.
 *
 * You can redistribute this application and/or modify it since you respect the terms of the GNU Affero General Public License.
 * If you modify the source code and then use this modified source code in your creation, you must make available the source code of your modifications.
 *
 * You should have received a copy of the GNU Affero General Public License along with the software.
 * If not, please see : <http://www.gnu.org/licenses/>. Full compliance requires reading the terms of this license and following its directives.

 */

package org.entcore.common.aggregation.processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.entcore.common.aggregation.indicators.Indicator;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

/**
 * The AggregationProcessing class launches the processing of values aggregated from ent-core traces.
 */
public abstract class AggregationProcessing {

	protected Collection<Indicator> indicators = new ArrayList<Indicator>();

	/**
	 * Creates a new AggregationProcessing instance.<br>
	 * Indicators can then be added by using the <code>addIndicator</code> method.
	 */
	public AggregationProcessing(){}
	/**
	 * Creates a new AggregationProcessing instance and sets the argument as the internal Indicator collection.
	 * @param indicatorsList : A collection of Indicators.
	 */
	public AggregationProcessing(Collection<Indicator> indicatorsList){
		this.indicators = indicatorsList;
	}

	/**
	 * Returns the list of indicators.
	 * @return List of indicators.
	 */
	public Collection<Indicator> getIndicators(){
		return indicators;
	}

	/**
	 * Adds a new Indicator to the collection and returns the AggregationProcessing object.
	 * @param i : An indicator.
	 * @return : This AggregationProcessing object.
	 */
	public AggregationProcessing addIndicator(Indicator i){
		this.indicators.add(i);
		return this;
	}

	/**
	 * Process the indicators as you see fit.
	 * @param callBack : Handler called when processing is over.
	 */
	public abstract void process(Handler<JsonObject> callBack);
	/**
	 * Process the indicators as you see fit.
	 * @param callBack : Handler called when processing is over.
	 * @param marker : Date marker, useful for an implementation relying on time.
	 */
	public abstract void process(Date marker, Handler<JsonObject> callBack);

}
