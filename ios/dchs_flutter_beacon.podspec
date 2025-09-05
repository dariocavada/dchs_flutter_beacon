#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint dchs_flutter_beacon.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'dchs_flutter_beacon'
  s.license = { :type => 'Apache', :file => '../LICENSE' }
  s.version          = '0.6.5'
  s.summary          = 'Flutter plugin for scanning beacon (iBeacon platform) devices on Android and iOS.'
  s.description      = <<-DESC
A comprehensive Flutter plugin that enables beacon ranging, monitoring, and broadcasting functionalities using CoreLocation and CoreBluetooth frameworks.
                       DESC
  s.homepage         = 'https://github.com/dariocavada/dchs_flutter_beacon'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Dario Cavada' => 'dario.cavada.lab@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files     = 'Classes/**/*'
  s.dependency       'Flutter'
  s.platform         = :ios, '13.0'

  # Aggiungi le framework CoreLocation e CoreBluetooth
  s.frameworks       = 'CoreLocation', 'CoreBluetooth'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 
    'DEFINES_MODULE' => 'YES', 
    'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' 
  }
  s.swift_version    = '5.0'

  # If your plugin requires a privacy manifest, for example if it uses any
  # required reason APIs, update the PrivacyInfo.xcprivacy file to describe your
  # plugin's privacy impact, and then uncomment this line. For more information,
  # see https://developer.apple.com/documentation/bundleresources/privacy_manifest_files
  # s.resource_bundles = {'dchs_flutter_beacon_privacy' => ['Resources/PrivacyInfo.xcprivacy']}
end
