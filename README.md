# SmartThings Repository

iBrew.groovy provides a SmartThings DeviceHandler that integrates with branch of iBrew (https://github.com/jonbur/iBrew) to provide control and monitoring of iKettle 


TeslaControl.py uses the Flask framework to provide a proxy to connect SmartThings to Tesla's API. Run it on a server on the same LAN segment as the SmartThings Hub. Requires teslajson (https://github.com/gglockner/teslajson), Flask (http://flask.pocoo.org/) and geopy (https://github.com/geopy/geopy)

tesladevice.groovy provides a SmartThings DeviceHandler that integrates with the above proxy

teslasmartheater.groovy is a SmartThings SmartApp that turns on the HVAC at a preset time on certain days if the selected vehicle is home and if outside temperature (as measured by SmartThings temperature sensor) is below a certain target.
