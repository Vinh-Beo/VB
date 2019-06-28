/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gmurru.bleframework;

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
public class RBLGattAttributes {
	private static HashMap<String, String> attributes = new HashMap<String, String>();
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";


	/*DSPS*/

	public static String BLE_SHIELD_SERVICE =  "0783b03e-8535-b5a0-7140-a304d2495cb7";
	public static String BLE_SHIELD_TX =       "0783b03e-8535-b5a0-7140-a304d2495cb8";
	public static String BLE_SHIELD_RX =       "0783b03e-8535-b5a0-7140-a304d2495cba";
	public static String BLE_SHIELD_LOW_CTRL = "0783b03e-8535-b5a0-7140-a304d2495cb9";



	static {
		// RBL Services.
		attributes.put(BLE_SHIELD_SERVICE, "BLE Shield Service");
		// RBL Characteristics.
		attributes.put(BLE_SHIELD_TX, "BLE Shield TX");
		attributes.put(BLE_SHIELD_RX, "BLE Shield RX");
		attributes.put(BLE_SHIELD_LOW_CTRL, "BLE Shield LOW CTRL");
	}

	public static String lookup(String uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}
