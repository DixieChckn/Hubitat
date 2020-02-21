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
 *    2020-02-21  Mike Cerwin     v0.02 - Initial Release
 * 
**/

metadata {
    definition (name: "Vizio SmartCast Input", namespace: "DixieChckn", author: "Mike Cerwin") {
        
        capability "Switch"
        capability "Momentary"

      }

preferences {
    section("Settings") {
        input(name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false)

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
    if (logEnable){runIn(1800, logsOff)}
}

def push() {
    
    def index = (device.deviceNetworkId.indexOf('input'))
    def jsonId = (device.deviceNetworkId.substring(index+5)) as int
    sendEvent(name: "switch", value: "on", isStateChange: true)
    runIn(1, toggleOff)
    if(logEnable){log.debug "jsonId: ${jsonId}"}
    if(logEnable){log.debug "${parent.getState()?.authCode}"}
    if(logEnable){log.debug "${parent.getState()?.devicePort}"}
    if(logEnable){log.debug "${parent.getState()?.deviceIp}"}
    
    if(logEnable){log.debug "Sending Current Input Hash Request to [${parent.getState()?.deviceIp}:${parent.getState()?.devicePort}]"}
    
        //Build Current Input Hash Request Parameters
        def currentInputRequestParams = [ 
          uri: "https://${parent.getState()?.deviceIp}:${parent.getState()?.devicePort}",
          path: "/menu_native/dynamic/tv_settings/devices/current_input",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${parent.getState()?.authCode}"],
          ignoreSSLIssues: true 
          ]
    try{
        //Send Current Input Hash Request
        httpGet(currentInputRequestParams) { resp ->
                    if (resp.success) {
                        currentHashVal = resp.data.ITEMS[0].HASHVAL
                        if(logEnable){log.debug "currentInput Response: ${resp.data}"}
                        if(logEnable){log.debug "Current Input Hash: ${currentHashVal}"}
                                      }
                                           }
     } catch (Exception e) {
        log.warn "Current Input Hash Request Failed: ${e.message}"
                           } 
    
        //Build New Input Value Request Parameters
        def newHashRequestParams = [ 
          uri: "https://${parent.getState()?.deviceIp}:${parent.getState()?.devicePort}",
          path: "/menu_native/dynamic/tv_settings/devices/name_input",
          contentType: "application/json",
          requestContentType: "application/json",
          headers: ["AUTH": "${parent.getState()?.authCode}"],
          ignoreSSLIssues: true 
          ]
    
        try{
            //Send New Input Value Request
            httpGet(newHashRequestParams) { resp ->
                    if (resp.success) {
                        newInputValue = resp.data.ITEMS[jsonId].NAME                      
                        if(logEnable){log.debug "inputList Response: ${resp.data}"}
                        if(logEnable){log.debug "New Input Value: ${newInputValue}"}           
                                      }
                                          }
     } catch (Exception e) {
        log.warn "New Input Value Request Failed: ${e.message}"
                           } 
    
        //Build Set Input Command Parameters
        def paramsForSetInput =[
          uri: "https://${parent.getState()?.deviceIp}:${parent.getState()?.devicePort}",
          path: "/menu_native/dynamic/tv_settings/devices/current_input",
          headers: ["AUTH": "${parent.getState()?.authCode}"],  
	      contentType: "application/json",
          body: "{\"REQUEST\": \"MODIFY\", \"VALUE\": \"${newInputValue}\", \"HASHVAL\": ${currentHashVal}}",
          ignoreSSLIssues: true    
          ]
    
        if(logEnable)log.debug "Set Input Command JSON: ${paramsForSetInput}"
    
        //Send Set Input Command
        try {
            httpPut(paramsForSetInput) { resp ->
               
                if(logEnable){log.debug "Set Input Command Response JSON: ${resp.data}"}
        }
     } catch (Exception e) {
        log.warn "Set Input Command Failed: ${e.message}"
   } 
    
} 


def toggleOff() {
    sendEvent(name: "switch", value: "off", isStateChange: true)
}

def on() {
    push()
}

def off() {
    push()
}