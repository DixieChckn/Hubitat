# [BETA] Vizio SmartCast Display Driver

This is an initial implementation of a device driver for Vizio SmartCast enabled displays.  It relies on local https requests issued directly to the display API to provide the device control.  Since the displays do not report status, you will need to rely on a polling routine to refresh the device state or call the refresh within your routine in RM when creating automations.              

##### Current Functionality:
- on/off
- mute/unmute
- volume up/down
- volume set
- channel up/down
- refresh

##### Not Yet Implemented:
- change input
- channel set
- other good stuff

##### Known Issues:
- Setting the volume level directly can cause erratic behavior when there is a soundbar or other audio device connected to and controlled by the HDMI ARC output of the display.  This may or may not be something that can be addressed in future releases as it is dependent on the specific audio components and the limitations of CEC.   


This driver uses elements from the SmartCast API documentation compiled by github user exviva (Travis LaMarr):  https://github.com/exiva/Vizio_SmartCast_API

## Requirements:  

1.  HE Platform Version 2.1.8 or later as it utilizes the "ignoreSSLIssues" functionality introduced in that release.
2.  Your physical device must have a Static IP or DHCP reservation.  This implementation does not include any device discovery functionality.  
3.  Your HE Hub must be able to communicate with with your display on port 7345 or 9000 depending on the firmware of the display.  If either the hub your display are in an isolated network or vlan you must configre your network appropriately to allow this communication.
4.  In order to control the on/off state of the display, the display must be in "Quick Start" mode.  If the display is set to "Eco Mode" the listening port is disabled when the display is off.  
. 

## Installation

1. Back up your hub and save a local copy before proceeding.

2. Import the driver from this repository into the "Drivers Code" section of Hubitat: 
    * Install driver code: https://raw.githubusercontent.com/DixieChckn/Hubitat/master/drivers/VizioSmartCastDisplay/Vizio-SmartCast-Display.groovy    


#### Initial Device Setup
  *You MUST have physical access to the device to complete the initial setup.  Pairing the driver with the display requires a numeric code that is displayed on-screen that must be entered into the driver settings to complete the pairing operation.  If the driver is not paired with the display it will not function.  
  
1.  Create a new Virtual Device and select the "Vizio SmartCast Display" driver
2.  Enter the IP Address of your SmartCast display in the "SmartCast IP Address" field under "Preferences"
3.  If your display is running firmware verison 4.0 or older, enter "9000" in the "SmartCast Port" field.  If your display is running a firmware newer than 4.0, use the default value of 7345
4.  Verify that "Enable Pairing" setting is turned on in the "Preferences" of the virtual device.
5.  "Save Preferences"
#### Pairing
1.  Ensure your Vizio Display is turned on and you can view the screen.
2.  Click or Tap the "Pair" command button in the virtual device 
3.  A numeric PIN code should appear on the screen of your Vizio Display.  This PIN code will only appear for a short time.  If the code disappears from the display before you complete step 9,  you will need to Click or Tap the "Pair" command again to display a new code.  
4.  Enter the code in the "pin" field of the "Complete Pairing" button in the device driver and then Click or Tap the button.  If pairing was successful, the PIN displayed on the screen of the display will disappear and you will see an entry for "authCode" in the "State Variables" section of the device driver.
5.  "Save Preferences"
6.  At this point I recommend turning off the "Enable Pairing" option in the "Preferences" and saving.  This will prevent the pairing code in the driver from running if the "Pair" command is unintentionally activated.  If you need to re-pair the device in the future.  Turn the "Enable Pairing" option on and "Save Preferences".  You can then begin the pairing process again.
7.  Done!

#### Multiple Hubs or Virtual Devices
If you plan to control the display from multiple HE Hubs or create multiple virtual devices to control the same display you will need to generate a unique "authCode" association for each instance.  This accomplished by modifying the "Pairing ID" and "Pairing Name" and saving the device preferences prior to starting the pairing process.  The "Pairing ID" must be numeric.  The "Pairing Name" should be alpha.  Do not use special characters in either.  These values should be unique for each hub or virtual device.  

## Other Stuff

- If enabled, Debug Logging can generate a copious amount of log data especially during status refreshes.  This is by desgn to assist with troubleshooting during the beta test period.  If enabled, it will disable itself after 30 minutes.  Unless you are experiencing a specific issue, I recommed leaving this setting disabled. 

- Testers needed for Channel Set and Change Input.  
I wrote this against my display (E60-E3) which does not have an integrated tuner.  I'll need testers to assist by providing JSON output from the channel portion of the API so I can parse it properly.  I'll provide a modified driver to collect this data when I begin to add that functionality to the driver. Similarly, the inputs on my display may not be the same as other displays and I'll need to collect JSON data for that output as well.     

- My goal for this driver is to have it work as universally as possible with as many SmartCast enabled models as possible.  As result, it may not ever control that one specific feature you're looking for, especially if it's unique to your display model.  Feel free to ask politely but understand that I may politely decline
