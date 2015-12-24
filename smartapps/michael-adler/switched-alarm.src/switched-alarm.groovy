/**
 *  Alarm enabled by switches
 *
 *  Copyright 2015 Michael Adler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Switched alarm",
    namespace: "michael-adler",
    author: "Michael Adler",
    description: "Trigger one alarm with another alarm, but only when one or more switches are on.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    section("Source alarm and switches") {
        input "srcAlarm", "capability.alarm", title: "Forward this alarm's events", multiple: false, required: true
        input "srcSwitches", "capability.switch", title: "When one of these switches is on", multiple: true, required: false
    }
    section("Destination alarm") {
        input "dstAlarms", "capability.alarm", title: "Control these alarms", multiple: true, required: true
        input "dstDelay", "number", title: "Delay destination alarm trigger (seconds)", multiple: false, required: false
        input "offAfter", "number", title: "Turn off after (seconds)", multiple: false, required: false
    }
}

def installed() {
    log.debug "Installed"

    initialize()
}

def updated() {
    log.debug "Updated"

    unsubscribe()
    initialize()
    dstAlarms?.off()
}

private initialize() {
    subscribe srcAlarm, "alarm", alarm
}

private alarm(evt) {
    log.debug "$evt.name: $evt.value"

    if (evt.value == "off") {
        dstAlarms?.off()
    }
    else {
        // If no switches defined then enable the destination siren. If switches are defined
        // then at least one must be on.
        def switch_is_on = (srcSwitches == null)
        srcSwitches.findAll {
            log.debug "$evt.name: Switch ${it.displayName} is ${it.currentValue("switch")}"
            switch_is_on = switch_is_on || (it.currentValue("switch") == "on")
        }

        def d = (dstDelay ? dstDelay : 0)

        if (switch_is_on) {
            log.debug "$evt.name: Triggering alarm $evt.value after $d seconds"
            switch (evt.value) {
                case "siren": dstAlarms?.siren(delay: d * 1000); break;
                case "strobe": dstAlarms?.strobe(delay: d * 1000); break;
                default: dstAlarms?.both(delay: d * 1000)
            }
        }

        // Turn off after specified time, whether or not switches are enabled
        if (offAfter) {
            log.debug "$evt.name: Off after $offAfter seconds"
            dstAlarms?.off(delay: (d + offAfter) * 1000)
            srcAlarm.off(delay: (d + offAfter) * 1000)
        }
    }
}
