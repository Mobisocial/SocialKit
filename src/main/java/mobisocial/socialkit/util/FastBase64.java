/*
 * Copyright (C) 2011 The Stanford MobiSocial Laboratory
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

package mobisocial.socialkit.util;

import android.os.Build;
import android.util.Base64;

public class FastBase64 {
	public static byte[] encode(byte[] data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encode(data, Base64.DEFAULT);
		return mobisocial.socialkit.util.Base64.encodeToByte(data, false);
	}
	public static String encodeToString(byte[] data) {
		if (data == null) {
			return "";
		}
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.encodeToString(data, Base64.DEFAULT);
		return mobisocial.socialkit.util.Base64.encodeToString(data, false);
	}
	public static byte[] decode(byte[] data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64.DEFAULT);
		return mobisocial.socialkit.util.Base64.decode(data);
	}
	public static byte[] decode(String data) {
		if(data == null)
			return new byte[0]; 
		if(Build.VERSION.SDK_INT >= 8)
			return Base64.decode(data, Base64. DEFAULT);
		return mobisocial.socialkit.util.Base64.decode(data);
	}
}
