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


/**
 * An Obj that has been encoded and signed by a {@link User} for sharing with
 * a {@link Feed}.
 *
 */
public interface SignedObj extends Obj {

    /**
     * Returns a hash of the user's signature over this obj.
     */
    public long getHash();

    /**
     * The application identified with the creation of this Obj.
     */
    public String getAppId();

    /**
     * Returns the {@link User} who signed this obj.
     * @return
     */
    public User getSender();

    /**
     * A user maintains a local sequence number indicating how many messages
     * he has sent to the feed given by {@link #getFeedName()}
     */
    public long getSequenceNumber();

    /**
     * The feed to which this Obj has been sent.
     * @return
     */
    public String getFeedName();
}
