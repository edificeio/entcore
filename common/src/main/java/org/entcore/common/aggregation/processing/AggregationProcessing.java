/*
 * Copyright © WebServices pour l'Éducation, 2016
 *
 * This file is part of ENT Core. ENT Core is a versatile ENT engine based on the JVM.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 * For the sake of explanation, any module that communicate over native
 * Web protocols, such as HTTP, with ENT Core is outside the scope of this
 * license and could be license under its own terms. This is merely considered
 * normal use of ENT Core, and does not fall under the heading of "covered work".
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
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
