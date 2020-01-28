# Vizio Smart Cast Display Driver for Hubitat [BETA]

This is an initial implementation of a device driver for Vizio SmartCast enabled displays.

This driver uses elements from the SmartCast API documentation compiled by Travis LaMarr:  https://github.com/exiva/Vizio_SmartCast_API

## Requirements:  

1.  HE Platform Version 2.1.8 or later
2.  Your physical device must have a Static IP or DHCP reservation.  This implementation does not include any device discovery functionality.

This driver makes use of the new "ignore SSL" feature to enable access to the SmartCast API which was previously inaccessible due to a self-signed cert enbedded in the device. 


## Installation

1. Back up your hub and save a local copy before proceeding.

2. Import the driver from this repository into the "Drivers Code" section of Hubitat: 
    * Install driver code: https://raw.githubusercontent.com/DixieChckn/Hubitat/master/drivers/VizioSmartCastDisplay/Vizio-SmartCast-Display.groovy    


### Initial Setup
  *You MUST have physical access to the device to complete the setup.  Pairing the driver with the display requires a numeric code that is displayed on-screen that must be entered into the driver to complete the pairing operation.  If the driver is not paired with the display it will not function.  

1.  Create a new Virtual Device and select the "Vizio SmartCast Display" Driver
2.  Enter the IP Address of your SmartCast display in the "SmartCast IP Address" field under "Preferences"
3.  If your display is running firmware verison 4.0 or older, enter "9000" in the "SmartCast Port" field.  If your display is running a firmware newer than 4.0, use the default value of 7345
4.  Ensure that "Enable Pairing" is turned on.
5.  "Save Preferences"
6.  Ensure your Vizio Display is turned on and you can view the screen.
7.  Click or Tap the "Pair" command button in the device driver
8.  A numeric PIN code should appear on the screen of your Vizio Display.  This PIN code will only appear for a short time.  If the code disappears from the display before you complete step 8,  you will need to click the "Pair" command again to display a new code.  
9.  Enter the code in the "pin" field of the "Complete Pairing" button in the device driver and then Click or Tap the button.  If pairing was successful, the PIN displayed on the screen of the display will disappear and you will see an entry for "authCode" in the "State Variables" section of the device driver.
10.  "Save Preferences"
11.  At this point I recommend turning off the "Enable Pairing" option in the "Preferences" and saving.  This will prevent the pairing code in the driver from running if the "Pair" command is unintentionally selected.  If you need to re-pair the device in the future.  Turn the "Enable Pairing" option on and "Save Preferences".  You can then begin the pairing process again.    


