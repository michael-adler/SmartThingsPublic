/**
 *  Switched Presence Sensor
 *
 *  Copyright 2020 Michael Adler
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

//
// Generate a device that is a presence sensor output with a controlling switch interface,
// allowing IFTTT rules to operate on switches that determine presence.
//

metadata {
    definition (name: "Switched Presence Sensor", namespace: "michael-adler", author: "Michael Adler") {
        capability "Presence Sensor"
        capability "Switch"
        capability "Actuator"
        capability "Sensor"
        capability "Health Check"
    }

    simulator {
        status "present": "presence: 1"
        status "not present": "presence: 0"
    }

    tiles {
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
            state("not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff")
            state("present", labelIcon:"st.presence.tile.present", backgroundColor:"#00A0DC")
        }
        standardTile("button", "device.switch", width: 2, height: 2, canChangeIcon: false) {
            state("off", label: 'Away', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "on")
            state("on", label: 'Present', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "off")
        }
        main "presence"
        details(["button", "presence"])
    }
}

// parse events into attributes
def parse(String description) {
    log.debug "Parsing '${description}'"

    def name = parseName(description)
    def value = parseValue(description)
    def linkText = getLinkText(device)
    def descriptionText = parseDescriptionText(linkText, value, description)
    def handlerName = getState(value)
    def isStateChange = isStateChange(device, name, value)

    def results = [
        name: name,
        value: value,
        unit: null,
        linkText: linkText,
        descriptionText: descriptionText,
        handlerName: handlerName,
        isStateChange: isStateChange,
        displayed: displayed(description, isStateChange)
        ]

    log.debug "Parse returned $results.descriptionText"
    return results
}

def installed() {
    log.trace "Executing 'installed'"
    off()
    initialize()
}

def updated() {
    log.trace "Executing 'updated'"
    initialize()
}

def initialize() {
    log.trace "Executing 'initialize'"
    sendEvent(name: "DeviceWatch-DeviceStatus", value: "online")
    sendEvent(name: "healthStatus", value: "online")
    sendEvent(name: "DeviceWatch-Enroll", value: [protocol: "cloud", scheme:"untracked"].encodeAsJson(), displayed: false)
}

// handle commands
def on() {
    log.debug "Executing 'on'"
    sendEvent(displayed: true, isStateChange: true, name: "presence", value: "present", descriptionText: "$device.displayName is present")
    sendEvent(name: "switch", value: "on")
}

def off() {
    log.debug "Executing 'off'"
    sendEvent(displayed: true, isStateChange: true, name: "presence", value: "not present", descriptionText: "$device.displayName is not present")
    sendEvent(name: "switch", value: "off")
}

private String parseName(String description) {
    if (description?.startsWith("presence: ")) {
        return "presence"
    }
    null
}

private String parseValue(String description) {
    log.debug "Executing 'parseValue' (${description})"
    switch (description) {
        case "presence: 1": return "present"
        case "presence: 0": return "not present"
        default: return description
    }
}

private parseDescriptionText(String linkText, String value, String description) {
    switch (value) {
        case "present": return "$linkText has arrived"
        case "not present": return "$linkText has left"
        default: return value
    }
}

private getState(String value) {
    switch (value) {
        case "present": return "arrived"
        case "not present": return "left"
        default: return value
    }
}
