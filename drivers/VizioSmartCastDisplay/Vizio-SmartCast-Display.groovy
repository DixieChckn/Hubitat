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
 *    2020-01-28  Mike Cerwin     v0.01 - Initial Release
 *    2020-02-21  Mike Cerwin     v0.02 - Added Input Child Device Functionality
 * 
 */
metadata {
    definition (name: "Vizio SmartCast Display", namespace: "DixieChckn", author: "Mike Cerwin", , importUrl: "https://raw.githubusercontent.com/DixieChckn/Hubitat/master/drivers/VizioSmartCastDisplay/Vizio-SmartCast-Display.groovy") {
        
        capability "Actuator"
        capability "TV"
        capability "Switch"
        capability "AudioVolume"
        capability "Refresh"
        
        attribute "mute", "bool"
        attribute "volume", "string"
        attribute "channel", "string"
        attribute "input", "string"

        command "pair"
        command "completePairing", ["pin"]

 }

preferences {
    section("Settings") {
        input(name: "deviceIp", type: "string", title:"SmartCast IP Address", description: "", defaultValue: "192.168.1.1", required: true)
		input(name: "devicePort", type: "string", title:"SmartCast Port", description: "", defaultValue: "7345", required: true)
        input(name: "pairingId", type: "int", title:"Pairing ID", description: "Hub ID for Pairing", defaultValue: "123456789", required: true)
        input(name: "pairingName", type: "string", title:"Pairing Name", description: "Hub Name for Pairing", defaultValue: "Hubitat", required: true)
        input(name: "createChildDevs", type: "bool", title: "Create Input child devices", defaultValue: false)
        input(name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false)
        input(name: "pairEnable", type: "bool", title: "Enable pairing", defaultValue: true)
     } 
    }
}
    

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def updated() {
    log.info "updated..."
    log.warn "debug logging is: ${logEnable == true}"
    state.deviceIp = deviceIp
    state.devicePort = devicePort
    if (logEnable){runIn(1800, logsOff)}
    if (createChildDevs) createChildDevices()
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
        
        if(logEnable)log.debug "pair Request JSON: ${paramsForPairing}"
        
        //Send Pairing Request
        try {
            httpPut(paramsForPairing) { resp ->
                if (resp.success) {
                    state.pairingToken = resp.data.ITEM.PAIRING_REQ_TOKEN
                }
                
                if (logEnable) log.debug "pair Response JSON: ${resp.data}"
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
        
        if(logEnable)log.debug "completePairing Request JSON: ${paramsForPairingComp}"
        
        
        //Send Pairing Completion COmmand
        try {
            httpPut(paramsForPairingComp) { resp ->
                if (resp.success) {
                state.authCode = resp.data.ITEM.AUTH_TOKEN
                }
                
                if(logEnable)log.debug "completePairing Response JSON: ${resp.data}"

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
    
        if(logEnable)log.debug "on Request JSON: ${paramsForOn}"
    
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
    
    if(logEnable)log.debug "off Request JSON: ${paramsForOff}"
    
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
    
        if(logEnable)log.debug "channelUp Request JSON: ${paramsForCu}"
    
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
    
        if(logEnable)log.debug "channelDown Request JSON: ${paramsForCd}"
    
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
    
    if(logEnable)log.debug "VolumeUp Request JSON: ${paramsForVu}"
    
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
    
   if(logEnable)log.debug "VolumeDown Request JSON: ${paramsForVd}"    
    
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
    
    if(logEnable)log.debug "volumeStatus Request JSON: ${paramsForVd}"
    
    try{
        //Send Volume Status Request
        httpGet(volRequestParams) { resp ->
                    if (resp.success) {
                		volHash = resp.data.ITEMS[8].HASHVAL
                        if(logEnable){ 
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
    
    if(logEnable)log.debug "setVolume Request JSON: ${paramsForSetVol}"
    
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
    
    if(logEnable)log.debug "mute Status Request JSON: ${muteRequestParams}"
    
    try{
        //Send Mute Status Request
        httpGet(muteRequestParams) { resp ->
                    if (resp.success) {
                		muteStatus = resp.data.ITEMS[9].VALUE.toLowerCase()
                        if (logEnable) log.debug "MuteStatus: ${muteStatus}"                                     
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
    
    if(logEnable)log.debug "mute Request JSON: ${paramsForMute}"
    
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
    
        if(logEnable)log.debug "unmute Status Request JSON: ${muteRequestParams}"    
    
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
                        
                        if(logEnable){
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
                if (resp.data.ITEMS.VALUE[0] == 0 && device.currentValue("switch") == "on"){
                        sendEvent(name: "switch", value: "off", isStateChange: true)}
                if (resp.data.ITEMS.VALUE[0] == 1 && device.currentValue("switch") == "off"){               
                        sendEvent(name: "switch", value: "on", isStateChange: true)}
                }
                if(logEnable)log.debug "Power State: ${resp.data.ITEMS.VALUE[0]}"
            }
        
     } catch (Exception e) {
        log.warn "Power Status Request Failed: ${e.message}"
   }
    
            //Build Current Input Hash Request Parameters
        def currentInputRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",,
          path: "/menu_native/dynamic/tv_settings/devices/current_input",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    try{
        //Send Current Input Hash Request
        httpGet(currentInputRequestParams) { resp ->
                    if (resp.success) {
                        if(device.currentValue("input") != resp.data.ITEMS[0].VALUE){
                        sendEvent(name: "input", value: "${resp.data.ITEMS[0].VALUE}", isStateChange: true)}
                        if(logEnable){log.debug "currentInput Response: ${resp.data}"}
                        if(logEnable){log.debug "Current Input Hash: ${currentHashVal}"}
                                      }
                                           }
     } catch (Exception e) {
        log.warn "Current Input Hash Request Failed: ${e.message}"
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
        //Send Status Request - Audio
        httpGet(audStatusRequestParams) { resp ->
                    if (resp.success) {
                       sendEvent(name: "volume", value: "${resp.data.ITEMS.VALUE[0]}", isStateChange: true)
                        
                        if(logEnable){
                            log.debug "Refesh Audio Status Response: ${resp.data}"
                            log.debug "Current Volume: ${device.currentValue("volume")}"}

            }
        }
     } catch (Exception e) {
        log.warn "Audio Status Request Failed: ${e.message}"
   } 
}


def createChildDevices() {
    
    def displayInput0 = "${device.deviceNetworkId}-input0"
    def displayInput1 = "${device.deviceNetworkId}-input1"
    def displayInput2 = "${device.deviceNetworkId}-input2"
    def displayInput3 = "${device.deviceNetworkId}-input3"
    def displayInput4 = "${device.deviceNetworkId}-input4"
    def displayInput5 = "${device.deviceNetworkId}-input5"
    def displayInput6 = "${device.deviceNetworkId}-input6"
    def displayInput7 = "${device.deviceNetworkId}-input7"
    def displayInput8 = "${device.deviceNetworkId}-input8"
    def displayInput9 = "${device.deviceNetworkId}-input9"
    
    if (logEnable) log.debug "Sending Input List Request to [${deviceIp}:${devicePort}]"
    
        //Build Input List Request Parameters
    def inputListRequestParams = [ 
          uri: "https://${deviceIp}:${devicePort}",
          path: "/menu_native/dynamic/tv_settings/devices/name_input",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${state.authCode}"],
          ignoreSSLIssues: true 
          ]
    try{
        //Send  Input List Request
        httpGet(inputListRequestParams) { resp ->
                    if (resp.success) {
                        
                        try{
                            if(resp.data.ITEMS[0].NAME){
                                input0Name = resp.data.ITEMS[0].NAME}
                        }catch(Exception e0){if(logEnable){log.error "Create Input Child Device Failed: ${e0.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[1].NAME){
                                input1Name = resp.data.ITEMS[1].NAME}
                        }catch(Exception e1){if(logEnable){log.error "Create Input Child Device Failed: ${e1.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[2].NAME){
                                input2Name = resp.data.ITEMS[2].NAME}
                        }catch(Exception e2){if(logEnable){log.error "Create Input Child Device Failed: ${e2.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[3].NAME){
                                input3Name = resp.data.ITEMS[3].NAME}
                        }catch(Exception e3){if(logEnable){log.error "Create Input Child Device Failed: ${e3.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[4].NAME){
                                input4Name = resp.data.ITEMS[4].NAME}
                        }catch(Exception e4){if(logEnable){log.error "Create Input Child Device Failed: ${e4.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[5].NAME){
                                input5Name = resp.data.ITEMS[5].NAME}
                        }catch(Exception e5){if(logEnable){log.error "Create Input Child Device Failed: ${e5.message}"}}
                        
                        
                        try{
                            if(resp.data.ITEMS[6].NAME){
                                input6Name = resp.data.ITEMS[6].NAME}
                        }catch(Exception e6){if(logEnable){log.error "Create Input Child Device Failed: ${e6.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[7].NAME){
                                input7Name = resp.data.ITEMS[7].NAME}
                        }catch(Exception e7){if(logEnable){log.error "Create Input Child Device Failed: ${e7.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[8].NAME){
                                input8Name = resp.data.ITEMS[8].NAME}
                        }catch(Exception e8){if(logEnable){log.error "Create Input Child Device Failed: ${e8.message}"}}
                        
                        try{
                            if(resp.data.ITEMS[9].NAME){
                                input9Name = resp.data.ITEMS[9].NAME}
                        }catch(Exception e9){if(logEnable){log.error "Create Input Child Device Failed: ${e9.message}"}}
                        
                        if(logEnable){log.debug "inputList Response: ${resp.data}"}  
                        
            }
        }
     } catch (Exception e) {
        log.warn "inputList Request Failed: ${e.message}"
   } 
    
    if(input0Name){
        if (!getChildDevice(displayInput0)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput0,
                                         ["label": "${device.displayName}-${input0Name}",
                                 "isComponent": false])

            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }

    if(input1Name){
        if (!getChildDevice(displayInput1)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput1,
                                         ["label": "${device.displayName}-${input1Name}",
                                 "isComponent": false])

            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
    
    
     if(input2Name){
        if (!getChildDevice(displayInput2)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput2,
                                         ["label": "${device.displayName}-${input2Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }

     if(input3Name){
        if (!getChildDevice(displayInput3)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput3,
                                         ["label": "${device.displayName}-${input3Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
    
     if(input4Name){
        if (!getChildDevice(displayInput4)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput4,
                                         ["label": "${device.displayName}-${input4Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
     }
    
     if(input5Name){
        if (!getChildDevice(displayInput5)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput5,
                                         ["label": "${device.displayName}-${input5Name}",
                                 "isComponent": false])
     
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
    
     if(input6Name){
        if (!getChildDevice(displayInput6)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput6,
                                         ["label": "${device.displayName}-${input6Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
    
     if(input7Name){
        if (!getChildDevice(displayInput7)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput7,
                                         ["label": "${device.displayName}-${input7Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
    
     if(input8Name){
        if (!getChildDevice(displayInput8)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput8,
                                         ["label": "${device.displayName}-${input8Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }

    if(input9Name){
        if (!getChildDevice(displayInput9)) {
            try {
                def dev = addChildDevice("Vizio SmartCast Input", displayInput9,
                                         ["label": "${device.displayName}-${input9Name}",
                                 "isComponent": false])
                
            } catch (Exception e) {
                log.error "Error creating SmartCast Input child device: $e"
            } 
        }
    }
}
