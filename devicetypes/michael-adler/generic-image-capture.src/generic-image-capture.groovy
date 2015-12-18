/**
 *  Generic Still Image Capture
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
 
//
// Derived from:
//     SmartThingsPublic/devicetypes/smartthings/foscam.src/foscam.groovy
// and
//     smartthings-ps/devicetypes/generic_camera.groovy
//

metadata {
	definition (name: "Generic Image Capture", namespace: "michael-adler", author: "Michael Adler") {
		capability "Image Capture"
		capability "Sensor"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	preferences {
 		input("CameraIP", "string", title:"Camera IP Address", description: "Please enter your camera's IP Address", required: true, displayDuringSetup: true)
		input("CameraPort", "string", title:"Camera Port", description: "Please enter your camera's Port", defaultValue: 80 , required: true, displayDuringSetup: true)
		input("CameraPath", "string", title:"Camera Path to Image", description: "Please enter the path to the image", defaultValue: "/SnapshotJPEG?Resolution=640x480&Quality=Clarity", required: true, displayDuringSetup: true)
		input("CameraPostGet", "string", title:"Does Camera use a Post or Get, normally Get?", description: "Please choose if the camera uses a POST or a GET command to retreive the image", defaultValue: "GET", displayDuringSetup: true)
		input("CameraAuth", "bool", title:"Does Camera require User Auth?", description: "Please choose if the camera requires authentication (only basic is supported)", defaultValue: true, displayDuringSetup: true)
 		input "CameraUsername", "text", title: "Camera Username", description: "Camera Username", required: true
		input "CameraPassword", "password", title: "Camera Password", description: "Camera Password", required: true
	}

	tiles {
		standardTile("image", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: true) {
			state "default", label: "", action: "", icon: "st.camera.dropcam-centered", backgroundColor: "#FFFFFF"
		}

		carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

		standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "take", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
			state "taking", label:'Taking', action: "", icon: "st.camera.dropcam", backgroundColor: "#53a7c0"
			state "image", label: "Take", action: "Image Capture.take", icon: "st.camera.dropcam", backgroundColor: "#FFFFFF", nextState:"taking"
		}

		main "image"
		details(["cameraDetails", "take"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

	def map = stringToMap(description)
	log.debug map

	def result = []

	if (map.bucket && map.key)
	{ //got a s3 pointer
		putImageInS3(map)
	}
	else if (map.headers && map.body)
	{ //got device info response
    	def body = new String(descMap["body"].decodeBase64())
        log.debug "Body: ${body}"
	}

	result
}

def putImageInS3(map) {

	def s3ObjectContent

	try {
		def imageBytes = getS3Object(map.bucket, map.key + ".jpg")

		if(imageBytes)
		{
			s3ObjectContent = imageBytes.getObjectContent()
			def bytes = new ByteArrayInputStream(s3ObjectContent.bytes)
			storeImage(getPictureName(), bytes)
		}
	}
	catch(Exception e) {
		log.error e
	}
	finally {
		//explicitly close the stream
		if (s3ObjectContent) { s3ObjectContent.close() }
	}
}

// handle commands
def take() {
	log.debug "Executing 'take'"

	def userpassascii = "${CameraUsername}:${CameraPassword}"
	def userpass = "Basic " + userpassascii.encodeAsBase64().toString()
    def host = CameraIP 
    def hosthex = convertIPtoHex(host)
    def porthex = convertPortToHex(CameraPort)
    device.deviceNetworkId = "$hosthex:$porthex" 
    
    log.debug "The device id configured is: $device.deviceNetworkId"
    
	def path = CameraPath
    log.debug "path is: $path"
    
    def headers = [:] 
    headers.put("HOST", "$host:$CameraPort")
	if (CameraAuth) {
		headers.put("Authorization", userpass)
    }
    
    log.debug "The Header is $headers"
    
    def method = "GET"
    log.debug "Thex method is $method"
    
//    try {
        def hubAction = new physicalgraph.device.HubAction(
    	    method: method,
    	    path: path,
    	    headers: headers
            )
        	
    	hubAction.options = [outputMsgToS3:true]
    	log.debug hubAction
    	return hubAction
//    }
//    catch (Exception e) {
//    	log.debug "Hit Exception $e on $hubAction"
//    }
}

//helper methods
private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	return device.deviceNetworkId + "_$pictureUuid" + ".jpg"
}

private getUsername() {
	settings.username
}

private getPassword() {
	settings.password
}

private String convertHexToIP(hex) {
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}
