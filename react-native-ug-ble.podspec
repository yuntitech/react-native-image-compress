

require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.description  = package['description']
  s.homepage     = package['homepage']
  s.license      = package['license']
  
  s.author       = { "yunti" => "notavailable@x.com" }
  s.platform     = :ios, "10.0"
  s.source       = { :git => "https://github.com/yuntitech/react-native-ug-Ble.git", :version => "0.0.3" }
  s.source_files  = "ios/**/*.{h,m}"
  s.requires_arc = true

  s.dependency "React"
  s.vendored_libraries = "ios/libBleDeviceTabletLib.a"
  s.ios.library = 'c++','c++abi'

end
