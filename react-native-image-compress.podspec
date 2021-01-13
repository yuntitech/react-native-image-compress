require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'react-native-image-compress'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = package['description']
  s.license        = package['license']
  s.author         = package['author']
  s.homepage       = package['homepage']
  s.source         = { :git => 'https://github.com/yuntitech/react-native-image-compress', :tag => "v#{s.version}" }

  s.requires_arc   = true
  s.platform       = :ios, '10.0'

  s.source_files = 'ios/*.{h,m}'

  s.dependency 'React-Core'
end
