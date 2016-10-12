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

package org.entcore.feeder.timetable;

import static fr.wseduc.webutils.Utils.isNotEmpty;

public class Slot {

	private final int start;
	private int end;

	public Slot(String start, String end, int slotDuration) {
		if (isNotEmpty(start)) {
			final String [] h = start.split("(h|:)");
			if (h.length == 3) {
				this.start = Integer.parseInt(h[0]) * 3600 + Integer.parseInt(h[1]) * 60 + Integer.parseInt(h[2]);
			} else if (h.length == 2) {
				this.start = Integer.parseInt(h[0]) * 3600 + Integer.parseInt(h[1]) * 60;
			} else {
				this.start = 0;
			}
		} else {
			this.start = 0;
		}
		if (isNotEmpty(end)) {
			final String [] h = end.split("(h|:)");
			if (h.length == 3) {
				this.end = Integer.parseInt(h[0]) * 3600 + Integer.parseInt(h[1]) * 60 + Integer.parseInt(h[2]);
			} else if (h.length == 2) {
				this.end = Integer.parseInt(h[0]) * 3600 + Integer.parseInt(h[1]) * 60;
			} else {
				this.end = 0;
			}
		} else {
			this.end = this.start + slotDuration;
		}
	}

	/**
	 *
	 * @return seconds between 00:00:00
	 */
	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	@Override
	public String toString() {
		return "" + start + " -> " + end;
	}

}
