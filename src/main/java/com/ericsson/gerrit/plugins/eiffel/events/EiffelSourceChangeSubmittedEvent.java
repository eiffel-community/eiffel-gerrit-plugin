/*
   Copyright 2019 Ericsson AB.
   For a full list of individual contributors, please see the commit history.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.ericsson.gerrit.plugins.eiffel.events;

import com.ericsson.gerrit.plugins.eiffel.events.models.EiffelSourceChangeSubmittedEventParams;
import com.ericsson.gerrit.plugins.eiffel.events.models.MsgParams;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This Eiffel event model represents the EiffelSourceChangeSubmittedEvent structure used by REMReM
 * and is populated with information from the change-merged gerrit event.
 *
 */
public class EiffelSourceChangeSubmittedEvent implements EiffelEvent {
    @SerializedName("msgParams")
    @Expose
    public MsgParams msgParams = new MsgParams();

    @SerializedName("eventParams")
    @Expose
    public EiffelSourceChangeSubmittedEventParams eventParams = new EiffelSourceChangeSubmittedEventParams();
}
