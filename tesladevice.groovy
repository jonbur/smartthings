import groovy.json.JsonSlurper

metadata {
definition (name: "Tesla-3", namespace: "ask4", author: "JB") {

    	capability "Polling"
	capability "Refresh"
	capability "Switch"
    	capability "presenceSensor"
   	capability "Lock"

	command "refresh"

	attribute "network","string"

}

preferences {
	input("ip", "text", title: "IP Address", description: "Local server IP address", required: true, displayDuringSetup: true)
	input("port", "number", title: "Port Number", description: "Port Number (Default:5000)", defaultValue: "5000", required: true, displayDuringSetup: true)
}

tiles (scale:2) {
	    multiAttributeTile(name:"toggle", type: "generic", width: 6, height: 4){
			tileAttribute ("device.lock", key: "PRIMARY_CONTROL") {
			attributeState "locked", label:'locked', action:"lock.unlock", icon:"st.locks.lock.locked", backgroundColor:"#79b821", nextState:"unlocking"
			attributeState "unlocked", label:'unlocked', action:"lock.lock", icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff", nextState:"locking"
			attributeState "unknown", label:"unknown", action:"lock.lock", icon:"st.locks.lock.unknown", backgroundColor:"#ffffff", nextState:"locking"
			attributeState "locking", label:'locking', icon:"st.locks.lock.locked", backgroundColor:"#79b821"
			attributeState "unlocking", label:'unlocking', icon:"st.locks.lock.unlocked", backgroundColor:"#ffffff"
		}
     }

     standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true) {
        state "on", label:'${name}', action:"switch.off", icon:"https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/heat_cool_icon.png", backgroundColor:"#79b821", nextState:"turningOff"
        state "off", label:'${name}', action:"switch.on", icon:"https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/heat_cool_icon.png", backgroundColor:"#ffffff", nextState:"turningOn"
        state "turningOn", label:'${name}', action:"switch.off", icon:"https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/heat_cool_icon.png", backgroundColor:"#79b821", nextState:"turningOff"
        state "turningOff", label:'${name}', action:"switch.on", icon:"https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/heat_cool_icon.png", backgroundColor:"#ffffff", nextState:"turningOn"
        state "offline", label:'${name}', icon:"https://raw.githubusercontent.com/tonesto7/nest-manager/master/Images/App/heat_cool_icon.png", backgroundColor:"#ff0000"
    }

    standardTile("refresh", "device.switch", inactiveLabel: false, height: 2, width: 2, decoration: "flat") {
        state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
    }

    standardTile("presence", "device.presence", width: 3, height: 2, canChangeBackground: true) {
		state("present", labelIcon:"st.presence.tile.mobile-present", backgroundColor:"#53a7c0")
		state("not present", labelIcon:"st.presence.tile.mobile-not-present", backgroundColor:"#ebeef2")
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
debug.log headerString
debug.log "Hello"
if (headerString.contains("200 OK")) {
	bodyString = new String(map.body.decodeBase64())
	slurper = new JsonSlurper()
	result = slurper.parseText(bodyString)
    log.debug "Here"
    log.debug result

    
	switch (result.isclimateon) {
		case "False":
			sendEvent(name: 'switch', value: "off" as String)
		break;
		case "True":
			sendEvent(name: 'switch', value: "on" as String)

		}
        
   switch (result.iscarlocked) {
        case "False":
        	log.debug 'Vehicle is unlocked'
        	sendEvent(name: 'lock', value: "unlocked" as String)
         break;
         case "True":
         	log.debug 'Vehicle is locked'
          	sendEvent(name: 'lock', value:  "lock" as String)
         }

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

def on() {
log.debug "Executing 'on'"
ipSetup()
api('on')
}

def off() {
log.debug "Executing 'off'"
ipSetup()
api('off')
}

def away() {
log.debug('not present')
sendEvent(name: 'presence', value: 'not present')
}

def present() {
log.debug('present')
sendEvent(name: 'presence', value: 'present')
}

def unlock() {
log.debug "Executing Unlock"
ipSetup()
api('unlock')
}

def lock() {
log.debug "Executing Lock"
ipSetup()
api('lock')
}

def poll() {
log.debug "Executing 'poll'"

if (device.deviceNetworkId != null) {
	refresh()
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
		rooPath = "/api/starthvac"
		log.debug "The start command was sent"
	break;
	case "off":
		rooPath = "/api/stophvac"
		log.debug "The stop command was sent"
	break;
	case "isclimateon":
		rooPath = "/api/isclimateon"
        log.debug "Request if climate is on sent"
    break;		
    case "ishome":
		rooPath = "/api/isvehiclehome"
		log.debug "Request if vehicle is home sent"
	break;
    case "unlock":
    	rooPath = "/api/doorunlock"
        log.debug "Unlocking Door"
    break;
    case "lock":
    	rooPath = "/api/doorlock"
        log.debug "Locking door"
    break;
    case "islocked":
    	rooPath = "/api/iscarlocked"
		log.debug "Request if car is locked sent"
    break;
    case "refresh":
    	rooPath = "/api/refresh"
		log.debug "request Refresh"
    break;

    }


switch (rooCommand) {
	case ["isclimateon", "ishome", "islocked", "refresh"]:
    	log.debug rooCommand
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
			), delayAction(1000), refresh()]
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
String hexip = ip.tokenize( '.' ).collect { String.format( '%02x', it.toInteger() ) }.join()
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
def text = "Version 0.21"
}

private def textCopyright() {
def text = "Copyright © 2016 JB"
}
