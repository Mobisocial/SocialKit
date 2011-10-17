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

package mobisocial.socialkit;

import android.net.Uri;

/**
 * <p>This interface will change to better separate Musubi from the POSI standards.
 * The new interface will look more like:
 * <ul>
 *   <li>public String getName()
 *   <li>public String getMembershipType(); // fixed, groupserver, owned, etc.
 * 
 * <p> 
 * Implementations will have other methods like getMembers(), addMember() etc.
 * <p>
 * Sending a message to a feed is done via an ObjTransport, using postObj(Feed feed, SignedObj obj).
 * <p>
 * Signing an Obj also requires access to the feed: SignedObj signed = Musubi.signObj(User from, Feed to, Obj obj);
 */
public interface Feed {
    public Uri getUri();
    public void postObj(Obj obj);
}
