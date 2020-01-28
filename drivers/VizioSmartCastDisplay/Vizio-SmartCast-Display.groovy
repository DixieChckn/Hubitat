/**
 *  Vizio SmartCast Display Driver
 *
 *  Copyright 2020 Mike Cerwin
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
 *  Change History:
 *
 *    Date        Who             What
 *    ----        ---             ----
*    2020-01-28   Mike Cerwin     v0.01
 * 
 */
metadata {
    definition (name: "Vizio SmartCast Display", namespace: "DixieChckn", author: "Mike Cerwin") {
        
        capability "Actuator"
        capability "TV"
        capability "Switch"
        capability "AudioVolume"
        capability "Refresh"
        
        attribute "mute", "bool"
        attribute "volume", "string"
        attribute "channel", "string"

        command "pair"
        command "completePairing", ["pin"]
        
    }
}

preferences {
    section("Settings") {
<<<<<<< HEAD
        input(name: "deviceIp", type: "string", title:"SmartCast IP Address", description: "", defaultValue: "192.168.1.1", required: true)
		input(name: "devicePort", type: "string", title:"SmartCast Port", description: "", defaultValue: "7345", required: true)
=======
        input(name: "deviceIp", type: "string", title:"SmartCast IP Address", description: "", defaultValue: "172.17.17.81", required: true)
	input(name: "devicePort", type: "string", title:"SmartCast Port", description: "", defaultValue: "7345", required: true)
>>>>>>> 7a919b6f694d23f093474360c8fdcf6e40f6640a
        input(name: "pairingId", type: "int", title:"Pairing ID", description: "Hub ID for Pairing", defaultValue: "123456789", required: true)
        input(name: "pairingName", type: "string", title:"Pairing Name", description: "Hub Name for Pairing", defaultValue: "Hubitat", required: true)
        input(name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false)
        input(name: "pairEnable", type: "bool", title: "Enable pairing", defaultValue: true)
        
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

def pair() { 
    
    if (pairEnable){
        
       if (logEnable) log.debug "Sending Pairing Command To [${deviceIp}:${devicePort}]"
        
        //Build Pairing Parameters
        def paramsForPairing =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/pairing/start", 
	      contentType: "application/json",
          body: "{\"DEVICE_ID\":\"${pairingId}\",\"DEVICE_NAME\":\"${pairingName}\"}",
          ignoreSSLIssues: true
          ]
        
        if(logenable)log.debug "pair Request JSON: ${paramsForPairing}"
        
        //Send Pairing Request
        try {
            httpPut(paramsForPairing) { resp ->
                if (resp.success) {
                    state.pairingToken = resp.data.ITEM.PAIRING_REQ_TOKEN
                }
                
                if (logenable) log.debug "pair Response JSON: ${resp.data}"
            }
        } catch (Exception e) {
            log.warn "pair Command Failed: ${e.message}"
        }  
    }
}

def completePairing(pin) {
    
    if (pairEnable){
        if (logEnable){ log.debug "Sending Pairing Completion Command To [${deviceIp}:${devicePort}]"
        log.debug "Pairing PIN: ${pin}"
        log.debug "Pairing Token: ${pairingToken}"}
        
        //Build Pairing Completion Request Parameters
        def paramsForPairingComp =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/pairing/pair", 
	      contentType: "application/json",
          body: "{\"DEVICE_ID\": \"${pairingId}\",\"CHALLENGE_TYPE\": 1,\"RESPONSE_VALUE\": \"${pin}\",\"PAIRING_REQ_TOKEN\": ${state.pairingToken}}",
          ignoreSSLIssues: true
          ]
        
        if(logenable)log.debug "completePairing Request JSON: ${paramsForPairingComp}"
        
        
        //Send Pairing Completion COmmand
        try {
            httpPut(paramsForPairingComp) { resp ->
                if (resp.success) {
                state.authCode = resp.data.ITEM.AUTH_TOKEN
                }
                
                if(logenable)log.debug "completePairing Response JSON: ${resp.data}"

            }
        } catch (Exception e) {
            log.warn "completePairing Command Failed: ${e.message}"
        }
    }
}

def on() {

    if (logEnable) log.debug "Sending Power On Command To [${deviceIp}:${devicePort}]"
      
        //Build Power On Parameters
        def paramsForOn =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 11, \"CODE\": 1, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
        if(logenable)log.debug "on Request JSON: ${paramsForOn}"
    
    //Send Power On Command
    try {
        httpPut(paramsForOn) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "on", isStateChange: true)
            }
            
            if (logEnable)log.debug "on Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "on Command Failed: ${e.message}"
   }
}

def off() {
    
    if (logEnable) log.debug "Sending Power Off Command to [${deviceIp}:${devicePort}]"
        
        //Build Power Off Parameters
        def paramsForOff =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 11, \"CODE\": 0, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
    if(logenable)log.debug "off Request JSON: ${paramsForOff}"
    
    //Send Power Off Command
    try {
        httpPut(paramsForOff) { resp ->
            if (resp.success) {
                sendEvent(name: "switch", value: "off", isStateChange: true)
            }
            
            if (logEnable)log.debug "off Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "off Command Failed: ${e.message}"
   }
}

def channelUp() {
    
    if (logEnable) log.debug "Sending Channel Up Command to [${deviceIp}:${devicePort}]"
        
        //Build Channel Up Parameters
        def paramsForCu =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 8, \"CODE\": 1, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
        if(logenable)log.debug "channelUp Request JSON: ${paramsForCu}"
    
    //Send Channel Up Command
    try {
        httpPut(paramsForCu) { resp ->
            if (resp.success) {
            }
            
            if (logEnable)log.debug "channelUp Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "channelUp Command Failed: ${e.message}"
   }
}

def channelDown() {
    
    if (logEnable) log.debug "Sending Channel Down Command to [${deviceIp}:${devicePort}]"
        
        //Build Channel Down Parameters
        def paramsForCd =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 8, \"CODE\": 0, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
        if(logenable)log.debug "channelDown Request JSON: ${paramsForCd}"
    
    //Send Channel Down Command
    try {
        httpPut(paramsForCd) { resp ->
            if (resp.success) {
            }
            
            if (logEnable)log.debug "channelDown Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "channelDown Command Failed: ${e.message}"
   }
}

def volumeUp() {
    
    if (logEnable) log.debug "Sending Volume Up Command to [${deviceIp}:${devicePort}]"
        
        //Build Volume Up Parameters
        def paramsForVu =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 5, \"CODE\": 1, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
    if(logenable)log.debug "VolumeUp Request JSON: ${paramsForVu}"
    
    //Send Volume Up Command
    try {
        httpPut(paramsForVu) { resp ->
            if (resp.success) { 
                runInMillis(900,refreshVol)
            }
            
            if (logEnable)log.debug "volumeUp Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "Volume Up Command Failed: ${e.message}"
   }


}


def volumeDown() {
    
    if (logEnable) log.debug "Sending Volume Down Command to [${deviceIp}:${devicePort}]"
        
        //Build Volume Down Parameters
        def paramsForVd =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 5, \"CODE\": 0, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
   if(logenable)log.debug "VolumeDown Request JSON: ${paramsForVd}"    
    
    //Send Volume Down Command
    try {
        httpPut(paramsForVd) { resp ->
            if (resp.success) { 
                runInMillis(900,refreshVol)
            }
            
            if (logEnable)log.debug "volumeDown Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "Volume Down Command Failed: ${e.message}"
   }
}

def setVolume(volumelevel) {
    
    if (logEnable) log.debug "Requesting Volume Status from [${deviceIp}:${devicePort}]"
    
        //Build Volume Status Request Parameters
        def volRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    
    if(logenable)log.debug "volumeStatus Request JSON: ${paramsForVd}"
    
    try{
        //Send Volume Status Request
        httpGet(volRequestParams) { resp ->
                    if (resp.success) {
                		volHash = resp.data.ITEMS[8].HASHVAL
                        if(logenable){ 
                            log.debug "Volume Hash Value: ${volHash}"
                            log.debug "Volume Level Value: ${volumelevel}"
                        }
            }
            
       if (logEnable)log.debug "volumeStatus Response JSON: ${resp.data}"     
            
         }
      } catch (Exception e) {
        log.warn "Volume Status Request Failed: ${e.message}"
    }
    
    if (logEnable) log.debug "Sending Set Volume to [${deviceIp}:${devicePort}]"
    
        //Build Set Volume Parameters
        def paramsForSetVol =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio/volume",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"REQUEST\": \"MODIFY\", \"VALUE\": ${volumelevel}, \"HASHVAL\": ${volHash}}",
          ignoreSSLIssues: true    
          ]
    
    if(logenable)log.debug "setVolume Request JSON: ${paramsForSetVol}"
    
    //Send Set Volume Command
    try {
        httpPut(paramsForSetVol) { resp ->
            
             if (logEnable) log.debug "volumeSet Response JSON: ${resp.data}"
        }
     } catch (Exception e) {
        log.warn "Set Volume Command Failed: ${e.message}"
   }
}

def mute() {
    
    if (logEnable) log.debug "Requesting Mute Status from [${deviceIp}:${devicePort}]"
    
        //Build Mute Status Request parameters
        def muteRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    
    if(logenable)log.debug "mute Status Request JSON: ${muteRequestParams}"
    
    try{
        //Send Mute Status Request
        httpGet(muteRequestParams) { resp ->
                    if (resp.success) {
                		muteStatus = resp.data.ITEMS[9].VALUE.toLowerCase()
                        if (logenable) log.debug "MuteStatus: ${muteStatus}"                                     
            }
            
            if (logEnable) log.debug "mute Status Response JSON: ${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Mute Status Request Failed: ${e.message}"
   }
     
    if (logEnable) log.debug "Sending Mute Command to [${deviceIp}:${devicePort}]"
    
        //Build Mute Parameters
        def paramsForMute =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 5, \"CODE\": 3, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
          ]
    
    if(logenable)log.debug "mute Request JSON: ${paramsForMute}"
    
    //Send Mute Command
    if (muteStatus == "off") {
    try {
        httpPut(paramsForMute) { resp ->
            if (resp.success) {
                sendEvent(name: "mute", value: "on", isStateChange: true)
            }
            
            if (logEnable) log.debug "mute Response JSON: ${resp.data}"
        }
      } catch (Exception e) {
        log.warn "Mute Command Failed: ${e.message}"
    }
  }
}

def unmute() {
    
    if (logEnable) log.debug "Sending Unmute Status request to [${deviceIp}:${devicePort}]"
    
        //Build Unmute Status Request parameters
        def unmuteRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    
        if(logenable)log.debug "unmute Status Request JSON: ${muteRequestParams}"    
    
    try{
        //Send Unmute Status Request
        httpGet(unmuteRequestParams) { resp ->
                    if (resp.success) {
                		muteStatus = resp.data.ITEMS[9].VALUE.toLowerCase()
                        //log.debug "MuteStatus: ${muteStatus}"                             
            }
            
            if (logEnable) log.debug "umute Status Response JSON: ${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Unmute Status Request Failed: ${e.message}"
    }
    
     if (logEnable) log.debug "Sending Unmute Command to [${deviceIp}:${devicePort}]"
        
        //Build Unmute Parameters
        def paramsForUnmute =[
          uri: "https://${deviceIp}:${devicePort}",
          path: "/key_command/",
          headers: ["AUTH": "${state.authCode}"],  
	      contentType: "application/json",
          body: "{\"KEYLIST\":[{\"CODESET\": 5, \"CODE\": 2, \"ACTION\": \"KEYPRESS\"}]}",
          ignoreSSLIssues: true
    ]
    //Send Unmute Command
    if (muteStatus == "on") {
    try {
        httpPut(paramsForUnmute) { resp ->
            if (resp.success) {
                sendEvent(name: "mute", value: "off", isStateChange: true)
            }
        }
     } catch (Exception e) {
        log.warn "unmute Command Failed: ${e.message}"
    }
  }
}

def refresh() {
    
    if (logEnable) log.debug "Sending Refresh Request to [${deviceIp}:${devicePort}]"
    
        //Build Status Request Parameters - Audio
        def audStatusRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    try{
        //Send Status Request
        httpGet(audStatusRequestParams) { resp ->
                    if (resp.success) {
                        
                        if(device.currentValue("volume") != resp.data.ITEMS[8].VALUE){
                            sendEvent(name: "volume", value: "${resp.data.ITEMS[8].VALUE}", isStateChange: true)}
                        
                        if(device.currentValue("mute") != resp.data.ITEMS[9].VALUE.toLowerCase()){
                            sendEvent(name: "mute", value: "${resp.data.ITEMS[9].VALUE.toLowerCase()}", isStateChange: true)}
                        
                        if(logenable){
                            log.debug "Refresh Audio Status Response: ${resp.data}"
                            log.debug "Current Volume: ${device.currentValue("volume")}"
                            log.debug "Current Mute State: ${device.currentValue("mute")}"}
            }
        }
     } catch (Exception e) {
        log.warn "Audio Status Request Failed: ${e.message}"
   }
        
        //Build Power Status Request Parameters
        def pwrStatusRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/state/device/power_mode",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true
          ]
    
    //Send Power Status Request
    try {
        httpGet(pwrStatusRequestParams) { resp ->
            if (resp.success) {
                if (resp.data.ITEMS.VALUE[0] == 0) {
                    sendEvent(name: "switch", value: "off", isStateChange: true)}
                if (resp.data.ITEMS.VALUE[0] == 1) {
                    sendEvent(name: "switch", value: "on", isStateChange: true)}
                
                if(logenable)log.debug "Power State: ${resp.data.ITEMS.VALUE[0]}"
            }
        }
     } catch (Exception e) {
        log.warn "Power Status Request Failed: ${e.message}"
   }
}

def refreshVol() {
    
    if (logEnable) log.debug "Sending Volume Refresh Request to [${deviceIp}:${devicePort}]"
    
        //Build Status Request Parameters - Audio
        def audStatusRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/audio/volume",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    try{
        //Send Status Request
        httpGet(audStatusRequestParams) { resp ->
                    if (resp.success) {
                       sendEvent(name: "volume", value: "${resp.data.ITEMS.VALUE[0]}", isStateChange: true)
                        
                        if(logenable){
                            log.debug "Refesh Audio Status Response: ${resp.data}"
                            log.debug "Current Volume: ${device.currentValue("volume")}"}

            }
        }
     } catch (Exception e) {
        log.warn "Audio Status Request Failed: ${e.message}"
   } 
}
