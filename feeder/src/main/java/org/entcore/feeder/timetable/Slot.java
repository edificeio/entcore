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
