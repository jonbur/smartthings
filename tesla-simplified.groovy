import groovy.json.JsonSlurper

metadata {
	definition (name: "Tesla-simplified", namespace: "ask4", author: "JB") {
		
        capability "Polling"
		capability "Refresh"
        capability "presenceSensor"

	}
    
	preferences {
		input("ip", "text", title: "IP Address", description: "Local server IP address", required: true, displayDuringSetup: true)
		input("port", "number", title: "Port Number", description: "Port Number (Default:5000)", defaultValue: "5000", required: true, displayDuringSetup: true)
	}

	tiles {

        standardTile("refresh", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("presence", "device.presence", width: 2, height: 2, canChangeBackground: true) {
			state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
			state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ebeef2")
	    }
        
        main "presence"
        details(["presence", "refresh"])
     }
 }

def parse(String description) {
	def map
	def headerString
	def bodyString
	def slurper
	def result

	map = stringToMap(description)
	headerString = new String(map.headers.decodeBase64())
	if (headerString.contains("200 OK")) {
		bodyString = new String(map.body.decodeBase64())
		slurper = new JsonSlurper()
		result = slurper.parseText(bodyString)
        log.debug result
     
      	switch (result.isvehiclehome) {
			case "False":
            	log.debug 'Vehicle is away'
				away()
			break;
			case "True":
            	log.debug 'Vehicle is home'
				present()
				
			}
            
    
		}
	else {
		sendEvent(name: 'status', value: "error" as String)
		log.debug headerString
	}
}

// handle commands

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	log.info "Tesla ${textVersion()} ${textCopyright()}"
	ipSetup()
	poll()
}


def away() {
	log.debug('not present')
	sendEvent(name: 'presence', value: 'not present')
}

def present() {
	log.debug('present')
	sendEvent(name: 'presence', value: 'present')
}



def poll() {
	log.debug "Executing 'poll'"
    
	if (device.deviceNetworkId != null) {
		refresh()
	}
	else {
		log.debug "DNI: Not set"
	}
}

def refresh() {
	log.debug "Executing 'refresh'"
	ipSetup()
    api('ishome')
}

def api(String rooCommand, success = {}) {
	def rooPath
	def hubAction
	
	switch (rooCommand) {
        case "ishome":
			rooPath = "/api/isvehiclehome"
			log.debug "Request if vehicle is home sent"
    }

	try {
			hubAction = new physicalgraph.device.HubAction(
			method: "GET",
			path: rooPath,
			headers: [HOST: "${settings.ip}:${settings.port}", Accept: "application/json"])
		}
		catch (Exception e) {
			log.debug "Hit Exception $e on $hubAction"
		}

    log.debug hubAction 
	return hubAction
}

def ipSetup() {
	def hosthex
	def porthex
	if (settings.ip) {
		hosthex = convertIPtoHex(settings.ip)
	}
	if (settings.port) {
		porthex = convertPortToHex(settings.port)
	}
	if (settings.ip && settings.port) {
		device.deviceNetworkId = "$hosthex:$porthex"
	}
}

private String convertIPtoHex(ip) { 
	String hexip = ip.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
	return hexip
}
private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
	return hexport
}

private def textVersion() {
	def text = "Version 0.1"
}

private def textCopyright() {
	def text = "Copyright © 2016 JB"
}
