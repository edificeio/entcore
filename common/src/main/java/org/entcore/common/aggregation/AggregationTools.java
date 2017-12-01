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

package org.entcore.common.aggregation;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;

import io.vertx.core.Handler;

public class AggregationTools {

	//Sets a calendar date to midnight, then returns a Date representation.
	public static Date setToMidnight(Calendar cal){
		return setHourTo(cal, 0);
	}
	
	public static Date setHourTo(Calendar cal, int hour){
		Calendar copycat = Calendar.getInstance();
		copycat.setTime(cal.getTime());
		copycat.set(Calendar.HOUR_OF_DAY, hour);
		copycat.set(Calendar.MINUTE, 0);
		copycat.set(Calendar.SECOND, 0);
		copycat.set(Calendar.MILLISECOND, 0);
		return copycat.getTime();
	}
	
	/**
	 * Allows chain calls to object methods expecting a Handler as an argument, in order to execute them one after another.
	 *
	 * @param <T> Type of items in the chain.
	 * @param <H> Handler type.
	 */
	public abstract static class HandlerChainer<T, H>{
		
		private LinkedList<T> chainList = new LinkedList<T>();
		
		/**
		 * Chains an item to the chainer.
		 * @param item : Item to be chained.
		 * @return This chainer object.
		 */
		public HandlerChainer<T, H> chainItem(T item) {
			chainList.addFirst(item);
			return this;
		}
		
		/**
		 * Method to be executed for each item in the chain.
		 * @param item : Item in the chain.
		 * @param nextCallback : Next item callback handler.
		 */
		protected abstract void executeItem(T item, final Handler<H> nextCallback);

		/**
		 * Execute the chain, then calls the callback handler.
		 * @param callBack : Handler called when the chain is complete.
		 */
		public void executeChain(final Handler<H> callBack) {
			Handler<H> backupHandler;
			ListIterator<T> iterator = chainList.listIterator();

			//No items in the list - callBack with null
			if (!iterator.hasNext()){
				callBack.handle(null);
				return;
			}

			final T firstItem = iterator.next();
			//Only 1 item in the list - execute it then callBack with the result
			if (!iterator.hasNext()){
				executeItem(chainList.getLast(), callBack);
				return;
			}
			
			//More than one items - execute each one, then call the next one with the previous result in the handler argument then callBack
			backupHandler = new Handler<H>() {
				public void handle(H handlingType) {
					executeItem(firstItem, callBack);
				}
			};

			while(iterator.hasNext()) {
				final T item = iterator.next();
				final Handler<H> handler = backupHandler;
				
				if(!iterator.hasNext())
					break;
				backupHandler = new Handler<H>() {
					public void handle(H handlingType) {
						executeItem(item, handler);
					}
				};
			}
			
			executeItem(chainList.getLast(), backupHandler);
		}
		
	}
}
