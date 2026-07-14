//
//  FBBluetoothStateHandler.h
//  flutter_beacon
//
//  Created by Alann Maulana on 24/08/19.
//

#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>

NS_ASSUME_NONNULL_BEGIN

@class DchsFlutterBeaconPlugin;
@interface FBBluetoothStateHandler : NSObject<FlutterStreamHandler>

@property (strong, nonatomic) DchsFlutterBeaconPlugin* instance;

- (instancetype) initWithFlutterBeaconPlugin:(DchsFlutterBeaconPlugin*) instance;

@end

NS_ASSUME_NONNULL_END
