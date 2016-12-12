import groovy.json.JsonSlurper

metadata {
	definition (name: "iBrew", namespace: "ask4", author: "JB") {
		capability "Polling"
		capability "Refresh"
		capability "Switch"
       	capability 	"Temperature Measurement"
        
		command "refresh"

		attribute "network","string"
		attribute "bin","string"
	}
    
	preferences {
		input("ip", "text", title: "IP Address", description: "iBrew Server Address", required: true, displayDuringSetup: true)
		input("port", "number", title: "Port Number", description: "Port Number (Default:80)", defaultValue: "2080", required: true, displayDuringSetup: true)
	}

	tiles {

		 standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
            state "on", label:'${name}', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#79b821", nextState:"turningOff"
            state "off", label:'${name}', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#ffffff", nextState:"turningOn"
            state "turningOn", label:'${name}', action:"switch.off", icon:"st.Home.home30", backgroundColor:"#79b821", nextState:"turningOff"
            state "turningOff", label:'${name}', action:"switch.on", icon:"st.Home.home30", backgroundColor:"#ffffff", nextState:"turningOn"
            state "offline", label:'${name}', icon:"st.Home.home30", backgroundColor:"#ff0000"
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("temperature", "device.temperature", inactiveLabel: false, height: 1, width: 1, decoration: "flat") {
            state "default", label:'${currentValue}°', unit:"C", icon:"st.Weather.weather2"
        }
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
        log.debug "Result" + result.sensors.status
		switch (result.sensors.status) {
			case "ready":
				sendEvent(name: 'switch', value: "off" as String)
			break;
			case "heating":
				sendEvent(name: 'switch', value: "on" as String)
				
			}
            
         log.debug "temp = " + result.sensors.temperature.raw.celsius
         sendEvent(name: "temperature", value: result.sensors.temperature.raw.celsius, unit: "C")
			
		}
	else {
		sendEvent(name: 'status', value: "error" as String)
		log.debug headerString
	}
	parse
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
	log.info "iBrew ${textVersion()} ${textCopyright()}"
	ipSetup()
	poll()
}

def on() {
	log.debug "Executing 'on'"
	ipSetup()
	api('on')
}

def off() {
	log.debug "Executing 'off'"
	api('off')
}



def poll() {
	log.debug "Executing 'poll'"
    
	if (device.deviceNetworkId != null) {
		api('refresh')
	}
	else {
		sendEvent(name: 'status', value: "error" as String)
		sendEvent(name: 'network', value: "Not Connected" as String)
		log.debug "DNI: Not set"
	}
}

def refresh() {
	log.debug "Executing 'refresh'"
	ipSetup()
	api('refresh')
}

def api(String rooCommand, success = {}) {
	def rooPath
	def hubAction
	
	switch (rooCommand) {
		case "on":
			rooPath = "/api/192.168.1.184/start"
			log.debug "The start command was sent"
		break;
		case "off":
			rooPath = "/api/192.168.1.184/stop"
			log.debug "The stop command was sent"
		break;
		case "refresh":
			rooPath = "/api/192.168.1.184/status"
			log.debug "The Status Command was sent"
		break;
	}
    
	switch (rooCommand) {
		case "refresh":
			try {
				hubAction = new physicalgraph.device.HubAction(
				method: "GET",
				path: rooPath,
				headers: [HOST: "${settings.ip}:${settings.port}", Accept: "application/json"])
			}
			catch (Exception e) {
				log.debug "Hit Exception $e on $hubAction"
			}
			break;
		default:
			try {
				hubAction = [new physicalgraph.device.HubAction(
				method: "GET",
				path: rooPath,
				headers: [HOST: "${settings.ip}:${settings.port}", Accept: "application/json"]
				), delayAction(1000), api('refresh')]
			}
			catch (Exception e) {
				log.debug "Hit Exception $e on $hubAction"
			}
			break;
	}
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
private delayAction(long time) {
	new physicalgraph.device.HubAction("delay $time")
}

private def textVersion() {
	def text = "Version 0.1"
}

private def textCopyright() {
	def text = "Copyright © 2016 JB. Based on Thinking Cleaner Copyright © 2016 pmjoen"
}
