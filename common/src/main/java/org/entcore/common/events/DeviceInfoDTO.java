/*
 * Copyright © "Open Digital Education", 2014
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

package org.entcore.common.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;

/**
 * Device information DTO for event enrichment.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInfoDTO {
	
	private final String osName;
	private final String osVersion;
	private final String deviceType;
	private final String deviceName;
	
	@JsonCreator
	public DeviceInfoDTO(
			@JsonProperty("osName") final String osName,
			@JsonProperty("osVersion") final String osVersion,
			@JsonProperty("deviceType") final String deviceType,
			@JsonProperty("deviceName") final String deviceName) {
		this.osName = osName;
		this.osVersion = osVersion;
		this.deviceType = deviceType;
		this.deviceName = deviceName;
	}
	
	public String getOsName() {
		return osName;
	}
	
	public String getOsVersion() {
		return osVersion;
	}
	
	public String getDeviceType() {
		return deviceType;
	}
	
	public String getDeviceName() {
		return deviceName;
	}
	
	
	@Override
	public String toString() {
		return "DeviceInfoDTO{" +
			"osName='" + osName + '\'' +
			", osVersion='" + osVersion + '\'' +
			", deviceType='" + deviceType + '\'' +
			", deviceName='" + deviceName + '\'' +
			'}';
	}
}
